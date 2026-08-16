import asyncio
from collections import deque
from dataclasses import dataclass
from datetime import datetime, timezone
import threading
import time

import numpy as np

from app.config import get_settings
from app.models import (
    VerificationRequest,
    VerificationResponse,
    VerificationResultEnum,
)
from app.services.face_recognition import (
    MODEL_NAME,
    MODEL_VERSION,
    calculate_similarity_percent,
    extract_face_embedding,
)
from app.services.liveness import BlinkDetector
from app.services.supabase_client import get_student_embedding


settings = get_settings()


@dataclass
class FramePacket:
    sequence: int
    received_at_ms: int
    frame: np.ndarray


class FrameBuffer:
    def __init__(self, max_len: int = 60, max_age_ms: int = 15_000):
        self._frames: deque[FramePacket] = deque(maxlen=max_len)
        self._max_age_ms = max_age_ms
        self._sequence = 0
        self._lock = threading.Lock()

    def add_frame(self, frame: np.ndarray) -> None:
        with self._lock:
            self._sequence += 1
            self._frames.append(
                FramePacket(
                    sequence=self._sequence,
                    received_at_ms=int(time.time() * 1000),
                    frame=frame,
                )
            )

    def packets_after(
        self,
        verification_started_ms: int,
        last_sequence: int,
    ) -> list[FramePacket]:
        now_ms = int(time.time() * 1000)
        with self._lock:
            return [
                packet
                for packet in self._frames
                if packet.sequence > last_sequence
                and packet.received_at_ms >= verification_started_ms
                and now_ms - packet.received_at_ms <= self._max_age_ms
            ]

    def latest_frame(self, max_age_ms: int = 3_000) -> np.ndarray | None:
        now_ms = int(time.time() * 1000)

        with self._lock:
            if not self._frames:
                return None

            packet = self._frames[-1]

            if now_ms - packet.received_at_ms > max_age_ms:
                return None

            return packet.frame.copy()


camera_buffers: dict[str, FrameBuffer] = {}
camera_locks: dict[str, asyncio.Lock] = {}
registry_lock = threading.Lock()

camera_capture_deadlines: dict[str, int] = {}
camera_capture_lock = threading.Lock()


def start_camera_capture(camera_id: str, duration_ms: int) -> None:
    deadline = int(time.time() * 1000) + duration_ms

    with camera_capture_lock:
        camera_capture_deadlines[camera_id] = deadline


def stop_camera_capture(camera_id: str) -> None:
    with camera_capture_lock:
        camera_capture_deadlines.pop(camera_id, None)


def is_camera_capture_active(camera_id: str) -> bool:
    now_ms = int(time.time() * 1000)

    with camera_capture_lock:
        deadline = camera_capture_deadlines.get(camera_id)

        if deadline is None:
            return False

        if now_ms > deadline:
            camera_capture_deadlines.pop(camera_id, None)
            return False

        return True


camera_last_seen_ms: dict[str, int] = {}
camera_last_seen_lock = threading.Lock()


def mark_camera_online(camera_id: str) -> None:
    with camera_last_seen_lock:
        camera_last_seen_ms[camera_id] = int(time.time() * 1000)


def is_camera_online(
    camera_id: str,
    timeout_ms: int = 3_000,
) -> bool:
    now_ms = int(time.time() * 1000)

    with camera_last_seen_lock:
        last_seen = camera_last_seen_ms.get(camera_id)

    if last_seen is None:
        return False

    return now_ms - last_seen <= timeout_ms


def get_buffer(camera_id: str) -> FrameBuffer:
    with registry_lock:
        if camera_id not in camera_buffers:
            camera_buffers[camera_id] = FrameBuffer()
        return camera_buffers[camera_id]


def get_camera_lock(camera_id: str) -> asyncio.Lock:
    with registry_lock:
        if camera_id not in camera_locks:
            camera_locks[camera_id] = asyncio.Lock()
        return camera_locks[camera_id]


def _response(
    request: VerificationRequest,
    result: VerificationResultEnum,
    similarity: float = 0.0,
    liveness: bool = False,
    reason: str | None = None,
) -> VerificationResponse:
    return VerificationResponse(
        verificationSessionId=request.sessionId,
        expectedUserId=request.expectedUserId,
        cameraId=request.cameraId,
        result=result,
        similarityPercent=round(similarity, 2),
        thresholdPercent=settings.face_similarity_threshold_percent,
        livenessPassed=liveness,
        modelName=MODEL_NAME,
        modelVersion=MODEL_VERSION,
        processedAt=datetime.now(timezone.utc),
        failureReason=reason,
    )


async def run_verification(request: VerificationRequest) -> VerificationResponse:
    expected_embedding = await get_student_embedding(str(request.expectedUserId))
    if expected_embedding is None:
        return _response(
            request,
            VerificationResultEnum.ERROR,
            reason="FACE_PROFILE_NOT_FOUND_OR_INACTIVE",
        )

    # Một camera chỉ xử lý một phiên tại một thời điểm.
    async with get_camera_lock(request.cameraId):
        started_ms = int(time.time() * 1000)
        capture_deadline = started_ms + request.captureTimeoutMs
        first_frame_deadline = started_ms + 2_500
        buffer = get_buffer(request.cameraId)
        detector = BlinkDetector()

        last_sequence = 0
        saw_fresh_frame = False
        saw_face = False
        blink_frame: np.ndarray | None = None

        try:
            while int(time.time() * 1000) <= capture_deadline:
                packets = buffer.packets_after(started_ms, last_sequence)

                for packet in packets:
                    last_sequence = packet.sequence
                    saw_fresh_frame = True
                    observation = await asyncio.to_thread(detector.update, packet.frame)
                    saw_face = saw_face or observation.face_detected

                    if observation.passed:
                        # Dùng chính frame hoàn tất blink làm frame đầu tiên để so khớp.
                        blink_frame = packet.frame
                        break

                if blink_frame is not None:
                    break

                now_ms = int(time.time() * 1000)
                if not saw_fresh_frame and now_ms >= first_frame_deadline:
                    return _response(
                        request,
                        VerificationResultEnum.CAMERA_OFFLINE,
                        reason="NO_FRESH_CAMERA_FRAME",
                    )

                await asyncio.sleep(0.05)
        finally:
            detector.close()

        if blink_frame is None:
            if saw_face:
                return _response(
                    request,
                    VerificationResultEnum.LIVENESS_FAILED,
                    reason="OPEN_CLOSED_OPEN_BLINK_NOT_COMPLETED",
                )
            return _response(
                request,
                VerificationResultEnum.CAPTURE_TIMEOUT,
                reason="NO_FACE_IN_CAPTURE_WINDOW",
            )

        match_deadline = int(time.time() * 1000) + request.matchTimeoutMs
        best_similarity = 0.0
        saw_valid_embedding = False
        frames_to_match = [blink_frame]

        while int(time.time() * 1000) <= match_deadline:
            packets = buffer.packets_after(started_ms, last_sequence)
            for packet in packets:
                last_sequence = packet.sequence
                frames_to_match.append(packet.frame)

            while frames_to_match:
                frame = frames_to_match.pop(0)
                face_result = await asyncio.to_thread(
                    extract_face_embedding,
                    frame,
                    False,
                )

                if int(time.time() * 1000) > match_deadline:
                    return _response(
                        request,
                        VerificationResultEnum.FACE_MATCH_TIMEOUT,
                        similarity=best_similarity,
                        liveness=True,
                        reason="FACE_MODEL_TIMEOUT",
                    )

                if face_result.error == "MULTIPLE_FACES":
                    return _response(
                        request,
                        VerificationResultEnum.ERROR,
                        similarity=best_similarity,
                        liveness=True,
                        reason="MULTIPLE_FACES",
                    )

                if face_result.embedding is None:
                    continue

                saw_valid_embedding = True
                similarity = calculate_similarity_percent(
                    expected_embedding,
                    face_result.embedding,
                )
                best_similarity = max(best_similarity, similarity)

                if best_similarity >= settings.face_similarity_threshold_percent:
                    return _response(
                        request,
                        VerificationResultEnum.VERIFIED,
                        similarity=best_similarity,
                        liveness=True,
                    )

            await asyncio.sleep(0.05)

        if saw_valid_embedding:
            return _response(
                request,
                VerificationResultEnum.FACE_BELOW_THRESHOLD,
                similarity=best_similarity,
                liveness=True,
                reason="SIMILARITY_BELOW_THRESHOLD",
            )

        return _response(
            request,
            VerificationResultEnum.FACE_MATCH_TIMEOUT,
            similarity=best_similarity,
            liveness=True,
            reason="NO_VALID_FACE_FOR_MATCHING",
        )
