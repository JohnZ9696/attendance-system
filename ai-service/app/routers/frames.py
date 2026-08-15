from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from typing import Annotated
import cv2
import numpy as np
from app.services.verification_manager import get_buffer
from app.config import get_settings


router = APIRouter(prefix="/internal/v1/cameras")
settings = get_settings()


@router.post("/{camera_id}/frames", tags=["frames"])
async def upload_frame(
    camera_id: str,
    request: Request,
    image: Annotated[UploadFile, File()],
):
    provided = request.headers.get("INTERNAL-API-KEY", "")
    if not settings.internal_api_key or not provided:
        raise HTTPException(status_code=401, detail="Invalid API key")
    if not __import__("secrets").compare_digest(provided, settings.internal_api_key):
        raise HTTPException(status_code=401, detail="Invalid API key")

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
