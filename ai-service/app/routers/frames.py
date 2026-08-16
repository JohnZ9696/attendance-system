from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from fastapi.responses import Response
from typing import Annotated
import cv2
import numpy as np
import secrets
from app.services.verification_manager import (
    get_buffer,
    is_camera_capture_active,
)
from app.config import get_settings


router = APIRouter(prefix="/internal/v1/cameras")
settings = get_settings()


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
    frame = get_buffer(camera_id).latest_frame()

    if frame is None:
        raise HTTPException(
            status_code=404,
            detail="CAMERA_OFFLINE",
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

    return {
        "cameraId": camera_id,
        "active": is_camera_capture_active(camera_id),
    }
