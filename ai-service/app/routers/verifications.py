import secrets

from fastapi import APIRouter, HTTPException, Request

from app.config import get_settings
from app.models import VerificationRequest, VerificationResponse
from app.services.verification_manager import (
    run_verification,
    start_camera_capture,
    stop_camera_capture,
)


router = APIRouter(prefix="/internal/v1/verifications")
settings = get_settings()


@router.post("", response_model=VerificationResponse, tags=["verifications"])
async def create_verification(
    payload: VerificationRequest,
    request: Request,
):
    provided = request.headers.get("INTERNAL-API-KEY", "")
    if not secrets.compare_digest(provided, settings.internal_api_key):
        raise HTTPException(status_code=401, detail="Invalid API key")

    capture_duration_ms = (
        payload.captureTimeoutMs
        + payload.matchTimeoutMs
        + 3_000
    )

    start_camera_capture(
        payload.cameraId,
        capture_duration_ms,
    )

    try:
        return await run_verification(payload)
    finally:
        stop_camera_capture(payload.cameraId)
