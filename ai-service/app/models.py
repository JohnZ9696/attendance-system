import enum
from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, Field


class VerificationResultEnum(str, enum.Enum):
    VERIFIED = "VERIFIED"
    CAPTURE_TIMEOUT = "CAPTURE_TIMEOUT"
    LIVENESS_FAILED = "LIVENESS_FAILED"
    FACE_BELOW_THRESHOLD = "FACE_BELOW_THRESHOLD"
    FACE_MATCH_TIMEOUT = "FACE_MATCH_TIMEOUT"
    CAMERA_OFFLINE = "CAMERA_OFFLINE"
    ERROR = "ERROR"


class EmbeddingResponse(BaseModel):
    embedding: list[float]
    faceCount: int
    blurScore: float


class VerificationRequest(BaseModel):
    sessionId: UUID
    expectedUserId: UUID
    cameraId: str = Field(min_length=1, max_length=100)
    captureTimeoutMs: int = Field(default=10_000, ge=1_000, le=20_000)
    matchTimeoutMs: int = Field(default=5_000, ge=1_000, le=10_000)


class VerificationResponse(BaseModel):
    verificationSessionId: UUID
    expectedUserId: UUID
    cameraId: str
    result: VerificationResultEnum
    similarityPercent: float
    thresholdPercent: float
    livenessPassed: bool
    modelName: str
    modelVersion: str
    processedAt: datetime
    failureReason: Optional[str] = None
