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
    image: Annotated[UploadFile, File()]
):
    """
    Upload a JPEG or PNG frame to a camera buffer.
    
    Parameters:
        camera_id (str): Identifier of the camera receiving the frame.
        image (UploadFile): JPEG or PNG image no larger than 5 MiB.
    
    Returns:
        dict: A status object containing ``{"status": "ok"}``.
    
    Raises:
        HTTPException: If authentication fails, the media type is unsupported,
            the payload exceeds 5 MiB, or the image content cannot be decoded.
    """
    api_key = request.headers.get("INTERNAL-API-KEY")
    if api_key != settings.internal_api_key:
        raise HTTPException(status_code=401, detail="Invalid API Key")
        
    if image.content_type not in ["image/jpeg", "image/png"]:
        raise HTTPException(status_code=415, detail="Unsupported media type")
        
    content = await image.read()
    if len(content) > 5 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Payload too large")
        
    # Decode image
    nparr = np.frombuffer(content, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    
    if frame is None:
        raise HTTPException(status_code=400, detail="Invalid image content")
        
    buffer = get_buffer(camera_id)
    buffer.add_frame(frame)
    
    return {"status": "ok"}
