from dataclasses import dataclass
import math
from statistics import median

import cv2
import numpy as np
from mediapipe.python.solutions import face_mesh as mp_face_mesh


LEFT_EYE = [362, 385, 387, 263, 373, 380]
RIGHT_EYE = [33, 160, 158, 133, 153, 144]

CALIBRATION_FRAMES = 3
CLOSED_EAR_RATIO = 0.5
REOPEN_EAR_RATIO = 0.98
MAX_MISSED_FACE_FRAMES = 8

REQUIRED_CLOSED_FRAMES = 1
REQUIRED_REOPEN_FRAMES = 1


@dataclass
class BlinkObservation:
    face_detected: bool
    ear: float
    state: str
    passed: bool


def _distance(point_a, point_b) -> float:
    return math.dist(
        (point_a.x, point_a.y),
        (point_b.x, point_b.y),
    )


def _eye_aspect_ratio(landmarks, indices) -> float:
    vertical_1 = _distance(
        landmarks[indices[1]],
        landmarks[indices[5]],
    )
    vertical_2 = _distance(
        landmarks[indices[2]],
        landmarks[indices[4]],
    )
    horizontal = _distance(
        landmarks[indices[0]],
        landmarks[indices[3]],
    )

    if horizontal <= 1e-6:
        return 0.0

    return (
        vertical_1 + vertical_2
    ) / (2.0 * horizontal)


class BlinkDetector:
    """Mỗi phiên xác thực tạo một detector riêng."""

    def __init__(self) -> None:
        self._mesh = mp_face_mesh.FaceMesh(
            static_image_mode=True,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.30,
            min_tracking_confidence=0.30,
        )

        self.reset()

    def reset(self) -> None:
        self.state = "CALIBRATING_OPEN"
        self.baseline_samples: list[float] = []
        self.baseline_ear = 0.0
        self.closed_threshold = 0.0
        self.reopen_threshold = 0.0
        self.closed_frames = 0
        self.reopen_frames = 0
        self.missed_face_frames = 0
        self.passed = False

    def update(self, frame: np.ndarray) -> BlinkObservation:
        rgb = cv2.cvtColor(
            frame,
            cv2.COLOR_BGR2RGB,
        )

        result = self._mesh.process(rgb)

        if not result.multi_face_landmarks:
            self.missed_face_frames += 1

            if self.missed_face_frames >= MAX_MISSED_FACE_FRAMES:
                self.reset()

            print(
                f"[LIVENESS] face=False "
                f"missed={self.missed_face_frames} "
                f"state={self.state}",
                flush=True,
            )

            return BlinkObservation(
                face_detected=False,
                ear=0.0,
                state=self.state,
                passed=False,
            )

        self.missed_face_frames = 0
        landmarks = result.multi_face_landmarks[0].landmark

        left_ear = _eye_aspect_ratio(landmarks, LEFT_EYE)
        right_ear = _eye_aspect_ratio(landmarks, RIGHT_EYE)
        ear = (left_ear + right_ear) / 2.0
        previous_state = self.state

        if self.state == "CALIBRATING_OPEN":
            self.baseline_samples.append(ear)

            if len(self.baseline_samples) >= CALIBRATION_FRAMES:
                self.baseline_ear = median(
                    self.baseline_samples
                )
                self.closed_threshold = (
                    self.baseline_ear * CLOSED_EAR_RATIO
                )
                self.reopen_threshold = (
                    self.baseline_ear * REOPEN_EAR_RATIO
                )
                self.state = "WAITING_CLOSED"

                print(
                    f"[LIVENESS CALIBRATED] "
                    f"baseline={self.baseline_ear:.4f} "
                    f"closedThreshold={self.closed_threshold:.4f} "
                    f"reopenThreshold={self.reopen_threshold:.4f}",
                    flush=True,
                )

        elif self.state == "WAITING_CLOSED":
            if ear <= self.closed_threshold:
                self.closed_frames += 1

                if self.closed_frames >= REQUIRED_CLOSED_FRAMES:
                    self.state = "WAITING_REOPEN"
                    self.reopen_frames = 0
            else:
                self.closed_frames = 0

        elif self.state == "WAITING_REOPEN":
            if ear >= self.reopen_threshold:
                self.reopen_frames += 1

                if self.reopen_frames >= REQUIRED_REOPEN_FRAMES:
                    self.state = "PASSED"
                    self.passed = True
            else:
                self.reopen_frames = 0

        print(
            f"[LIVENESS] "
            f"ear={ear:.4f} "
            f"baseline={self.baseline_ear:.4f} "
            f"closedThreshold={self.closed_threshold:.4f} "
            f"reopenThreshold={self.reopen_threshold:.4f} "
            f"state={previous_state}->{self.state} "
            f"passed={self.passed}",
            flush=True,
        )

        return BlinkObservation(
            face_detected=True,
            ear=round(ear, 4),
            state=self.state,
            passed=self.passed,
        )

    def close(self) -> None:
        self._mesh.close()