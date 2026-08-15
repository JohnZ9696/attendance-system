from fastapi import APIRouter

router = APIRouter()

@router.get("/health", tags=["health"])
def health_check():
    """Return the health status and service identifier."""
    return {"status": "ok", "service": "ai-service"}
