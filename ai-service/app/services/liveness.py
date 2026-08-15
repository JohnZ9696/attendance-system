import cv2
import mediapipe as mp
import numpy as np
import math

mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(
    max_num_faces=1,
    refine_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

# Eye landmarks points
LEFT_EYE = [362, 385, 387, 263, 373, 380]
RIGHT_EYE = [33, 160, 158, 133, 153, 144]
EAR_THRESHOLD = 0.21

def euclidean_distance(p1, p2):
    """
    Calculate the 2D Euclidean distance between two landmark points.
    
    Parameters:
    	p1: The first landmark point.
    	p2: The second landmark point.
    
    Returns:
    	float: The Euclidean distance between the points.
    """
    return math.dist([p1.x, p1.y], [p2.x, p2.y])

def get_ear(landmarks, eye_indices):
    # vertical
    """
    Calculate the eye aspect ratio from eye landmark coordinates.
    
    Parameters:
        landmarks: Landmark points used to measure the eye.
        eye_indices: Indices identifying the eye's horizontal and vertical landmarks.
    
    Returns:
        The eye aspect ratio, or 0 if the horizontal eye distance is zero.
    """
    v1 = euclidean_distance(landmarks[eye_indices[1]], landmarks[eye_indices[5]])
    v2 = euclidean_distance(landmarks[eye_indices[2]], landmarks[eye_indices[4]])
    # horizontal
    h = euclidean_distance(landmarks[eye_indices[0]], landmarks[eye_indices[3]])
    
    if h == 0:
        return 0
    ear = (v1 + v2) / (2.0 * h)
    return ear

def process_liveness(frame: np.ndarray) -> bool:
    """
    Determine whether the detected face is blinking or has closed eyes.
    
    Parameters:
        frame (np.ndarray): BGR image frame to analyze.
    
    Returns:
        bool: `True` if the average eye aspect ratio is below the blink threshold, `False` if no face is detected or the eyes are open.
    """
    img_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = face_mesh.process(img_rgb)
    
    if not results.multi_face_landmarks:
        return False
        
    landmarks = results.multi_face_landmarks[0].landmark
    
    left_ear = get_ear(landmarks, LEFT_EYE)
    right_ear = get_ear(landmarks, RIGHT_EYE)
    
    avg_ear = (left_ear + right_ear) / 2.0
    
    return avg_ear < EAR_THRESHOLD
