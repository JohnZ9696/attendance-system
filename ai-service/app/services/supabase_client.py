from supabase import create_client, Client
from app.config import get_settings
from typing import Optional, List
import json

settings = get_settings()
supabase: Client = create_client(settings.supabase_url, settings.supabase_service_key)

async def get_student_embedding(user_id: str) -> Optional[List[float]]:
    try:
        response = supabase.table("students").select("face_embedding, is_active").eq("id", user_id).execute()
        if not response.data:
            return None
            
        student = response.data[0]
        if not student.get("is_active"):
            return None
            
        embedding_data = student.get("face_embedding")
        if isinstance(embedding_data, str):
            return json.loads(embedding_data)
        elif isinstance(embedding_data, list):
            return embedding_data
            
        return None
    except Exception as e:
        print(f"Error fetching from Supabase: {e}")
        return None
