import os
from typing import Annotated

from fastapi import FastAPI, File, HTTPException, UploadFile


app = FastAPI(
    title="Attendance AI Service",
    description="Image analysis service used by the attendance system.",
    version="0.1.0",
)


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok", "service": "ai-service"}


@app.post("/api/v1/analyze", tags=["analysis"])
async def analyze_image(
    image: Annotated[UploadFile, File(description="Face image to analyze")],
) -> dict[str, object]:
    """Validate an image upload and return the future analysis contract.

    Face detection, liveness, and identity matching will be added here. Keeping
    the endpoint contract in place lets the Spring Boot service integrate first.
    """
    allowed_types = {"image/jpeg", "image/png", "image/webp"}
    if image.content_type not in allowed_types:
        raise HTTPException(status_code=415, detail="Only JPEG, PNG, and WebP images are supported")

    content = await image.read()
    if not content:
        raise HTTPException(status_code=400, detail="The uploaded image is empty")

    return {
        "matched": False,
        "liveness": False,
        "confidence": 0.0,
        "status": "not_implemented",
        "filename": image.filename,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_SERVICE_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_SERVICE_PORT", "8000")),
        reload=os.getenv("AI_SERVICE_RELOAD", "false").lower() == "true",
    )
