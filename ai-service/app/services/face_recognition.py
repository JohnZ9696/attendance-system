from dataclasses import dataclass
from typing import Optional

import cv2
import logging
import numpy as np
from deepface import DeepFace


MODEL_NAME = "Facenet512"
MODEL_VERSION = "deepface-0.0.93"
EXPECTED_DIMENSION = 512

logger = logging.getLogger("uvicorn.error")


@dataclass
class FaceEmbeddingResult:
    embedding: Optional[list[float]]
    face_count: int
    blur_score: float
    error: Optional[str] = None


def extract_face_embedding(
    frame: np.ndarray,
    require_enrollment_quality: bool = False,
) -> FaceEmbeddingResult:
    if frame is None or frame.size == 0:
        return FaceEmbeddingResult(None, 0, 0.0, "INVALID_IMAGE")

    height, width = frame.shape[:2]
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # Điểm tạm thời của toàn ảnh. Sau khi phát hiện khuôn mặt,
    # giá trị này sẽ được tính lại trên riêng vùng khuôn mặt.
    blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())

    if require_enrollment_quality and min(height, width) < 240:
        return FaceEmbeddingResult(
            None,
            0,
            blur_score,
            "IMAGE_TOO_SMALL",
        )

    try:
        results = DeepFace.represent(
            img_path=frame,
            model_name=MODEL_NAME,
            detector_backend="opencv",
            enforce_detection=True,
            align=True,
            normalization="Facenet",
        )
    except ValueError as exception:
        logger.warning("Khong phat hien khuon mat: %s", exception)
        return FaceEmbeddingResult(
            None,
            0,
            blur_score,
            "NO_FACE",
        )
    except Exception:
        logger.exception("DeepFace model error")
        return FaceEmbeddingResult(
            None,
            0,
            blur_score,
            "MODEL_ERROR",
        )

    if len(results) != 1:
        return FaceEmbeddingResult(
            None,
            len(results),
            blur_score,
            "MULTIPLE_FACES",
        )

    result = results[0]

    # Tính độ nét trên vùng khuôn mặt thay vì toàn bộ ảnh.
    area = result.get("facial_area") or {}
    x = max(0, int(area.get("x", 0)))
    y = max(0, int(area.get("y", 0)))
    w = int(area.get("w", width))
    h = int(area.get("h", height))

    face_gray = gray[
        y:min(y + h, height),
        x:min(x + w, width),
    ]

    if face_gray.size > 0:
        blur_score = float(
            cv2.Laplacian(face_gray, cv2.CV_64F).var()
        )

    logger.info(
        "Face detected: count=%d blur_score=%.2f area=%s",
        len(results),
        blur_score,
        area,
    )

    # Ngưỡng 50 phù hợp hơn để kiểm thử bằng ảnh web/ESP32-CAM.
    if require_enrollment_quality and blur_score < 50.0:
        return FaceEmbeddingResult(
            None,
            1,
            blur_score,
            "IMAGE_TOO_BLURRY",
        )

    embedding = [
        float(value)
        for value in result["embedding"]
    ]

    if len(embedding) != EXPECTED_DIMENSION:
        return FaceEmbeddingResult(
            None,
            1,
            blur_score,
            "INVALID_EMBEDDING_DIMENSION",
        )

    return FaceEmbeddingResult(
        embedding,
        1,
        blur_score,
    )


def calculate_similarity_percent(
    expected_embedding: list[float],
    current_embedding: list[float],
) -> float:
    expected = np.asarray(expected_embedding, dtype=np.float32)
    current = np.asarray(current_embedding, dtype=np.float32)

    if expected.shape != current.shape or expected.size == 0:
        return 0.0

    denominator = float(np.linalg.norm(expected) * np.linalg.norm(current))
    if denominator == 0.0:
        return 0.0

    cosine_similarity = float(np.dot(expected, current) / denominator)
    return round(max(0.0, min(100.0, cosine_similarity * 100.0)), 2)
