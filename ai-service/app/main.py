import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import frames, verifications, health

app = FastAPI(
    title="Attendance AI Service",
    description="Face recognition and liveness detection service",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(frames.router)
app.include_router(verifications.router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_SERVICE_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_SERVICE_PORT", "8000")),
        reload=os.getenv("AI_SERVICE_RELOAD", "false").lower() == "true",
    )
