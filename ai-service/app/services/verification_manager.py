import time
import asyncio
from typing import Dict, List, Tuple
from collections import deque
import numpy as np

from app.models import VerificationRequest, VerificationResponse, VerificationResultEnum
from app.services.supabase_client import get_student_embedding
from app.services.face_recognition import get_face_embedding, calculate_similarity_percent
from app.services.liveness import process_liveness
from app.config import get_settings
from datetime import datetime, timezone

settings = get_settings()

class FrameBuffer:
    def __init__(self, max_len=30, max_age_ms=15000):
        """
        Initialize a bounded frame buffer with a maximum frame age.
        
        Parameters:
        	max_len (int): Maximum number of frames the buffer can hold.
        	max_age_ms (int): Maximum age of stored frames in milliseconds.
        """
        self.frames = deque(maxlen=max_len)
        self.max_age_ms = max_age_ms
        
    def add_frame(self, frame: np.ndarray):
        """Store a camera frame with its current timestamp."""
        now_ms = int(time.time() * 1000)
        self.frames.append((now_ms, frame))
        
    def get_recent_frames(self) -> List[np.ndarray]:
        """
        Return frames captured within the configured maximum age.
        """
        now_ms = int(time.time() * 1000)
        # Filter old frames
        valid = [f for t, f in self.frames if now_ms - t <= self.max_age_ms]
        return valid

# In-memory storage of frames per camera
camera_buffers: Dict[str, FrameBuffer] = {}

def get_buffer(camera_id: str) -> FrameBuffer:
    """Retrieve the frame buffer associated with a camera, creating it when necessary.
    
    Parameters:
    	camera_id (str): Identifier of the camera.
    
    Returns:
    	FrameBuffer: The camera's frame buffer.
    """
    if camera_id not in camera_buffers:
        camera_buffers[camera_id] = FrameBuffer()
    return camera_buffers[camera_id]

async def run_verification(req: VerificationRequest) -> VerificationResponse:
    """
    Verify a user against camera frames using face similarity and blink-based liveness.
    
    Parameters:
    	req (VerificationRequest): Verification request containing the expected user, camera, session, and timeout details.
    
    Returns:
    	VerificationResponse: Verification result with similarity, liveness status, model metadata, and failure reason when applicable.
    """
    start_time = int(time.time() * 1000)
    timeout_ms = req.timeoutMs or settings.capture_liveness_timeout_ms
    
    # 1. Fetch expected embedding
    expected_embedding = await get_student_embedding(str(req.expectedUserId))
    
    def create_resp(result, sim=0.0, liveness=False, reason=None):
        """
        Create a verification response populated with session, user, camera, similarity, liveness, and model details.
        
        Parameters:
            result: Verification outcome.
            sim: Face similarity percentage.
            liveness: Whether liveness verification passed.
            reason: Optional explanation for a failed verification.
        
        Returns:
            VerificationResponse: A structured verification result.
        """
        return VerificationResponse(
            verificationSessionId=req.sessionId,
            expectedUserId=req.expectedUserId,
            cameraId=req.cameraId,
            result=result,
            similarityPercent=sim,
            thresholdPercent=settings.face_similarity_threshold_percent,
            livenessPassed=liveness,
            modelName="Facenet512",
            modelVersion="1.0",
            processedAt=datetime.now(timezone.utc),
            failureReason=reason
        )

    if not expected_embedding:
        return create_resp(VerificationResultEnum.ERROR, reason="User embedding not found or inactive")
        
    buffer = get_buffer(req.cameraId)
    
    liveness_passed = False
    best_sim = 0.0
    has_valid_face = False
    face_match_start_time = None
    
    while True:
        now_ms = int(time.time() * 1000)
        elapsed = now_ms - start_time
        
        if elapsed > timeout_ms:
            if not liveness_passed and not has_valid_face:
                return create_resp(VerificationResultEnum.CAPTURE_TIMEOUT, reason="No face or blink detected in time")
            if has_valid_face and not liveness_passed:
                return create_resp(VerificationResultEnum.LIVENESS_FAILED, sim=best_sim, reason="Liveness (blink) failed")
            if liveness_passed and has_valid_face:
                return create_resp(VerificationResultEnum.FACE_BELOW_THRESHOLD, sim=best_sim, liveness=True, reason="Similarity below threshold")
            
        # Process latest frames in buffer
        frames = buffer.get_recent_frames()
        if not frames:
            await asyncio.sleep(0.1)
            continue
            
        # We'll just look at the most recent frame added for speed
        frame = frames[-1]
        
        # 1. Check liveness (blink)
        if not liveness_passed:
            is_blinking = process_liveness(frame)
            if is_blinking:
                liveness_passed = True
                
        # 2. Check face embedding
        current_embedding = get_face_embedding(frame)
        if current_embedding:
            has_valid_face = True
            if face_match_start_time is None:
                face_match_start_time = now_ms
                
            sim = calculate_similarity_percent(expected_embedding, current_embedding)
            if sim > best_sim:
                best_sim = sim
                
            if best_sim >= settings.face_similarity_threshold_percent and liveness_passed:
                return create_resp(VerificationResultEnum.VERIFIED, sim=best_sim, liveness=True)
                
        # Check face match timeout if we found a face but haven't succeeded yet
        if has_valid_face and (now_ms - face_match_start_time) > settings.face_matching_timeout_ms:
            # Matching timeout occurred
            if liveness_passed:
                if best_sim >= settings.face_similarity_threshold_percent:
                    return create_resp(VerificationResultEnum.VERIFIED, sim=best_sim, liveness=True)
                else:
                    return create_resp(VerificationResultEnum.FACE_BELOW_THRESHOLD, sim=best_sim, liveness=True, reason="Similarity below threshold")
            else:
                return create_resp(VerificationResultEnum.LIVENESS_FAILED, sim=best_sim, reason="Liveness (blink) failed")
                
        await asyncio.sleep(0.05)
