# AI Service

FastAPI service for image analysis in the attendance system.

## Run locally

First, navigate to the `ai-service` directory:

```bash
cd ai-service
```

### 1. Setup virtual environment and dependencies

**Using `uv` (Recommended - much faster):**
```bash
# Create virtual environment
uv venv

# Install dependencies
uv pip install -r requirements.txt
```

**Using standard Python tools:**
```bash
# 1. Create virtual environment
python -m venv .venv

# 2. Activate it
# On Linux/macOS:
source .venv/bin/activate
# On Windows (Command Prompt):
.venv\Scripts\activate.bat
# On Windows (PowerShell):
.venv\Scripts\Activate.ps1

# 3. Install dependencies
pip install -r requirements.txt
```

### 2. Set up environment variables

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

Open `.env` in your text editor and ensure you provide values for at least:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_KEY`

### 3. Run the server

**Using `uv`:**
```bash
uv run python -m app.main
```

**Using standard Python tools (ensure venv is activated):**
```bash
python -m app.main
```

The service listens on `http://0.0.0.0:8000`. Interactive API documentation
is available at `http://0.0.0.0:8000/docs`.

## Simulation

If you do not have an actual ESP32-CAM device, you can use the `webcam_sender.py` script to simulate one using your computer's webcam.

**Using `uv`:**
```bash
uv run python webcam_sender.py
```

**Using standard Python tools (ensure venv is activated):**
```bash
python webcam_sender.py
```

## Endpoints

- `GET /health` checks service availability.
- `POST /api/v1/analyze` accepts an image in the `image` multipart field.

The analysis endpoint currently validates the upload and returns a stable
placeholder response until the face recognition model is connected.
