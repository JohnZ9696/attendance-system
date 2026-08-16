import logging
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
logger = logging.getLogger("uvicorn.error")


@router.post("", response_model=VerificationResponse)
async def create_verification(
    payload: VerificationRequest,
    request: Request,
):
    provided_key = request.headers.get(
        "INTERNAL-API-KEY",
        "",
    )

    if not secrets.compare_digest(
        provided_key,
        settings.internal_api_key,
    ):
        raise HTTPException(
            status_code=401,
            detail="Invalid API key",
        )

    logger.info(
        "[VERIFICATION REQUEST] camera=%s session=%s user=%s",
        payload.cameraId,
        payload.sessionId,
        payload.expectedUserId,
    )

    duration_ms = (
        payload.captureTimeoutMs
        + payload.matchTimeoutMs
        + 3_000
    )

    start_camera_capture(
        payload.cameraId,
        duration_ms,
    )

    logger.info(
        "[CAPTURE START] camera=%s duration=%d",
        payload.cameraId,
        duration_ms,
    )

    try:
        return await run_verification(payload)
    finally:
        stop_camera_capture(payload.cameraId)

        logger.info(
            "[CAPTURE STOP] camera=%s",
            payload.cameraId,
        )