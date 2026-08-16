from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from fastapi.responses import Response
from typing import Annotated
import cv2
import logging
import numpy as np
import secrets
from app.services.verification_manager import (
    get_buffer,
    is_camera_capture_active,
    is_camera_online,
    mark_camera_online,
)
from app.config import get_settings


router = APIRouter(prefix="/internal/v1/cameras")
settings = get_settings()
logger = logging.getLogger("uvicorn.error")


def check_internal_api_key(request: Request) -> None:
    provided = request.headers.get("INTERNAL-API-KEY", "")

    if not secrets.compare_digest(
        provided,
        settings.internal_api_key,
    ):
        raise HTTPException(
            status_code=401,
            detail="Invalid API key",
        )


@router.post("/{camera_id}/frames", tags=["frames"])
async def upload_frame(
    camera_id: str,
    request: Request,
    image: Annotated[UploadFile, File()],
):
    check_internal_api_key(request)
    mark_camera_online(camera_id)

    if image.content_type not in {"image/jpeg", "image/png"}:
        raise HTTPException(status_code=415, detail="Only JPEG or PNG is accepted")

    content = await image.read()
    if not content or len(content) > settings.max_image_bytes:
        raise HTTPException(status_code=413, detail="Image is empty or larger than 5 MB")

    # Decode image
    nparr = np.frombuffer(content, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if frame is None:
        raise HTTPException(status_code=400, detail="Invalid image content")

    get_buffer(camera_id).add_frame(frame)

    return {"status": "ok"}


@router.get("/{camera_id}/preview.jpg", tags=["frames"])
async def latest_camera_frame(camera_id: str):
    frame = get_buffer(camera_id).latest_frame(
        max_age_ms=5_000,
    )

    if frame is None:
        raise HTTPException(
            status_code=404,
            detail="NO_FRESH_CAMERA_FRAME",
        )

    encoded_ok, encoded = cv2.imencode(
        ".jpg",
        frame,
        [int(cv2.IMWRITE_JPEG_QUALITY), 80],
    )

    if not encoded_ok:
        raise HTTPException(
            status_code=500,
            detail="FRAME_ENCODE_FAILED",
        )

    return Response(
        content=encoded.tobytes(),
        media_type="image/jpeg",
        headers={
            "Cache-Control":
                "no-store, no-cache, must-revalidate, max-age=0",
            "Pragma": "no-cache",
        },
    )


@router.get("/{camera_id}/capture-command", tags=["frames"])
async def get_capture_command(
    camera_id: str,
    request: Request,
):
    check_internal_api_key(request)

    mark_camera_online(camera_id)
    active = is_camera_capture_active(camera_id)

    logger.info(
        "[CAPTURE COMMAND] camera=%s active=%s",
        camera_id,
        active,
    )

    return {
        "cameraId": camera_id,
        "active": active,
    }


@router.get("/{camera_id}/status", tags=["frames"])
async def get_camera_status(camera_id: str):
    online = is_camera_online(camera_id)
    capture_active = is_camera_capture_active(camera_id)
    latest_frame = get_buffer(camera_id).latest_frame(
        max_age_ms=5_000,
    )

    return {
        "cameraId": camera_id,
        "online": online,
        "captureActive": capture_active,
        "hasPreview": latest_frame is not None,
    }
