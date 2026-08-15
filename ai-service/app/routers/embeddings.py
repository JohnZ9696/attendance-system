import asyncio
import secrets
from typing import Annotated

import cv2
import numpy as np
from fastapi import APIRouter, File, HTTPException, Request, UploadFile

from app.config import get_settings
from app.models import EmbeddingResponse
from app.services.face_recognition import extract_face_embedding


router = APIRouter(prefix="/internal/v1/face-embeddings")
settings = get_settings()


def _check_api_key(request: Request) -> None:
    provided = request.headers.get("INTERNAL-API-KEY", "")
    if not secrets.compare_digest(provided, settings.internal_api_key):
        raise HTTPException(status_code=401, detail="Invalid API key")


@router.post("", response_model=EmbeddingResponse, tags=["face-enrollment"])
async def create_embedding(
    request: Request,
    image: Annotated[UploadFile, File()],
):
    _check_api_key(request)

    if image.content_type not in {"image/jpeg", "image/png"}:
        raise HTTPException(status_code=415, detail="Only JPEG or PNG is accepted")

    content = await image.read()
    if not content or len(content) > settings.max_image_bytes:
        raise HTTPException(status_code=413, detail="Image is empty or larger than 5 MB")

    array = np.frombuffer(content, dtype=np.uint8)
    frame = cv2.imdecode(array, cv2.IMREAD_COLOR)
    if frame is None:
        raise HTTPException(status_code=400, detail="Invalid image")

    result = await asyncio.to_thread(
        extract_face_embedding,
        frame,
        True,
    )

    if result.error:
        status = 422 if result.error != "MODEL_ERROR" else 500
        raise HTTPException(status_code=status, detail=result.error)

    return EmbeddingResponse(
        embedding=result.embedding,
        faceCount=result.face_count,
        blurScore=round(result.blur_score, 2),
    )