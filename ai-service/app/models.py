from pydantic import BaseModel, Field
from typing import Optional
from datetime import datetime
from uuid import UUID
import enum

class VerificationResultEnum(str, enum.Enum):
    VERIFIED = "VERIFIED"
    CAPTURE_TIMEOUT = "CAPTURE_TIMEOUT"
    LIVENESS_FAILED = "LIVENESS_FAILED"
    FACE_BELOW_THRESHOLD = "FACE_BELOW_THRESHOLD"
    FACE_MATCH_TIMEOUT = "FACE_MATCH_TIMEOUT"
    CAMERA_OFFLINE = "CAMERA_OFFLINE"
    ERROR = "ERROR"

class VerificationRequest(BaseModel):
    sessionId: UUID
    expectedUserId: UUID
    cameraId: str
    timeoutMs: Optional[int] = Field(default=None, description="Optional override for timeout")

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
