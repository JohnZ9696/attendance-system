from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

class Settings(BaseSettings):
    supabase_url: str = "https://dtdgwzvpquvpyttptlim.supabase.co"
    supabase_service_key: str
    internal_api_key: str
    
    face_similarity_threshold_percent: float = 30.0
    capture_liveness_timeout_ms: int = 10000
    face_matching_timeout_ms: int = 5000
    
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

@lru_cache
def get_settings() -> Settings:
    """Create and return the application settings instance."""
    return Settings()
