from dataclasses import dataclass
import math

import cv2
import numpy as np
from mediapipe.python.solutions import face_mesh as mp_face_mesh


LEFT_EYE = [362, 385, 387, 263, 373, 380]
RIGHT_EYE = [33, 160, 158, 133, 153, 144]

CLOSED_EAR_THRESHOLD = 0.20
OPEN_EAR_THRESHOLD = 0.24
REQUIRED_OPEN_FRAMES = 2
REQUIRED_CLOSED_FRAMES = 2
REQUIRED_REOPEN_FRAMES = 2


@dataclass
class BlinkObservation:
    face_detected: bool
    ear: float
    state: str
    passed: bool


def _distance(point_a, point_b) -> float:
    return math.dist((point_a.x, point_a.y), (point_b.x, point_b.y))


def _eye_aspect_ratio(landmarks, indices) -> float:
    vertical_1 = _distance(landmarks[indices[1]], landmarks[indices[5]])
    vertical_2 = _distance(landmarks[indices[2]], landmarks[indices[4]])
    horizontal = _distance(landmarks[indices[0]], landmarks[indices[3]])
    if horizontal <= 1e-6:
        return 0.0
    return (vertical_1 + vertical_2) / (2.0 * horizontal)


class BlinkDetector:
    """Mỗi phiên xác thực phải tạo một BlinkDetector riêng."""

    def __init__(self) -> None:
        self._mesh = mp_face_mesh.FaceMesh(
            static_image_mode=False,
            max_num_faces=1,
            refine_landmarks=True,
            min_detection_confidence=0.60,
            min_tracking_confidence=0.60,
        )
        self.state = "WAITING_OPEN"
        self.open_frames = 0
        self.closed_frames = 0
        self.reopen_frames = 0
        self.passed = False

    def reset(self) -> None:
        self.state = "WAITING_OPEN"
        self.open_frames = 0
        self.closed_frames = 0
        self.reopen_frames = 0
        self.passed = False

    def update(self, frame: np.ndarray) -> BlinkObservation:
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        result = self._mesh.process(rgb)

        if not result.multi_face_landmarks:
            self.reset()
            return BlinkObservation(False, 0.0, self.state, False)

        landmarks = result.multi_face_landmarks[0].landmark
        left_ear = _eye_aspect_ratio(landmarks, LEFT_EYE)
        right_ear = _eye_aspect_ratio(landmarks, RIGHT_EYE)
        ear = (left_ear + right_ear) / 2.0

        if self.state == "WAITING_OPEN":
            if ear >= OPEN_EAR_THRESHOLD:
                self.open_frames += 1
                if self.open_frames >= REQUIRED_OPEN_FRAMES:
                    self.state = "WAITING_CLOSED"
            else:
                self.open_frames = 0

        elif self.state == "WAITING_CLOSED":
            if ear <= CLOSED_EAR_THRESHOLD:
                self.closed_frames += 1
                if self.closed_frames >= REQUIRED_CLOSED_FRAMES:
                    self.state = "WAITING_REOPEN"
            elif ear >= OPEN_EAR_THRESHOLD:
                self.closed_frames = 0

        elif self.state == "WAITING_REOPEN":
            if ear >= OPEN_EAR_THRESHOLD:
                self.reopen_frames += 1
                if self.reopen_frames >= REQUIRED_REOPEN_FRAMES:
                    self.state = "PASSED"
                    self.passed = True
            elif ear <= CLOSED_EAR_THRESHOLD:
                self.reopen_frames = 0

        return BlinkObservation(True, round(ear, 4), self.state, self.passed)

    def close(self) -> None:
        self._mesh.close()
