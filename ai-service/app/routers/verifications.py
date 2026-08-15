from fastapi import APIRouter, Request, HTTPException
from app.models import VerificationRequest, VerificationResponse
from app.services.verification_manager import run_verification
from app.config import get_settings

router = APIRouter(prefix="/internal/v1/verifications")
settings = get_settings()

@router.post("", tags=["verifications"], response_model=VerificationResponse)
async def create_verification(req: VerificationRequest, request: Request):
    api_key = request.headers.get("INTERNAL-API-KEY")
    if api_key != settings.internal_api_key:
        raise HTTPException(status_code=401, detail="Invalid API Key")
        
    result = await run_verification(req)
    return result

@router.get("/{session_id}", tags=["verifications"])
async def get_verification(session_id: str, request: Request):
    api_key = request.headers.get("INTERNAL-API-KEY")
    if api_key != settings.internal_api_key:
        raise HTTPException(status_code=401, detail="Invalid API Key")
    # For now, verification is mostly synchronous / running in the POST handler
    raise HTTPException(status_code=501, detail="Not implemented async polling")
