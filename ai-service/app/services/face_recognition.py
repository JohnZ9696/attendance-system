import numpy as np
import cv2
from deepface import DeepFace
from app.config import get_settings

settings = get_settings()

MODEL_NAME = "Facenet512"

def get_face_embedding(frame: np.ndarray):
    try:
        # Convert BGR to RGB for DeepFace
        img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # enforce_detection=True will raise an exception if no face is found
        results = DeepFace.represent(img_path=img_rgb, model_name=MODEL_NAME, enforce_detection=True)
        if results and len(results) > 0:
            # Return first detected face embedding
            return results[0]["embedding"]
    except Exception as e:
        # No face found or other error
        pass
    return None

def calculate_similarity_percent(embedding1, embedding2) -> float:
    # Cosine distance = 1 - cosine_similarity
    # similarity_percent = cosine_similarity * 100
    
    vec1 = np.array(embedding1)
    vec2 = np.array(embedding2)
    
    dot_product = np.dot(vec1, vec2)
    norm_a = np.linalg.norm(vec1)
    norm_b = np.linalg.norm(vec2)
    
    if norm_a == 0 or norm_b == 0:
        return 0.0
        
    cosine_similarity = dot_product / (norm_a * norm_b)
    
    # Cosine similarity is between -1 and 1
    # Convert to percentage 0-100
    percent = cosine_similarity * 100.0
    
    # Clamp
    return float(max(0.0, min(100.0, percent)))
