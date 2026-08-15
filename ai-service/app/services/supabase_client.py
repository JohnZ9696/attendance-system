import asyncio
import json
from typing import Optional

from supabase import Client, create_client

from app.config import get_settings


settings = get_settings()
supabase: Client = create_client(
    settings.supabase_url,
    settings.supabase_service_key,
)


def _load_embedding(user_id: str) -> Optional[list[float]]:
    response = (
        supabase.table("students")
        .select("face_embedding,is_active")
        .eq("id", user_id)
        .limit(1)
        .execute()
    )

    if not response.data:
        return None

    student = response.data[0]
    if student.get("is_active") is False:
        return None

    value = student.get("face_embedding")
    if isinstance(value, str):
        value = json.loads(value)

    if not isinstance(value, list) or len(value) != 512:
        return None

    return [float(item) for item in value]


async def get_student_embedding(user_id: str) -> Optional[list[float]]:
    try:
        return await asyncio.to_thread(_load_embedding, user_id)
    except Exception:
        return None
