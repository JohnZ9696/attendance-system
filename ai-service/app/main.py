import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import embeddings, frames, health, verifications
from app.services.udp_discovery import start_udp_discovery


logger = logging.getLogger("uvicorn.error")


@asynccontextmanager
async def lifespan(_: FastAPI):
    settings = get_settings()
    transport = None
    try:
        transport = await start_udp_discovery(
            settings.ai_discovery_port,
            settings.ai_service_port,
        )
    except OSError as error:
        logger.warning("AI UDP discovery failed: %s", error)

    try:
        yield
    finally:
        if transport is not None:
            transport.close()

app = FastAPI(
    title="Attendance AI Service",
    description="Face embedding, recognition and blink liveness",
    version="1.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(embeddings.router)
app.include_router(frames.router)
app.include_router(verifications.router)

if __name__ == "__main__":
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.ai_service_host,
        port=settings.ai_service_port,
        reload=settings.ai_service_reload,
    )
