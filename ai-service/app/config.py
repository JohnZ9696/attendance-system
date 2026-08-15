from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    supabase_url: str
    supabase_service_key: str
    internal_api_key: str

    face_similarity_threshold_percent: float = 30.0
    capture_liveness_timeout_ms: int = 10_000
    face_matching_timeout_ms: int = 5_000
    max_image_bytes: int = 5 * 1024 * 1024

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
