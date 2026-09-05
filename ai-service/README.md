# AI Service

FastAPI service for image analysis in the attendance system.

## Run locally

First, navigate to the `ai-service` directory:

```bash
cd ai-service
```

### 1. Create a virtual environment

```bash
python -m venv .venv
```

### 2. Activate the virtual environment

**On Linux/macOS:**
```bash
source .venv/bin/activate
```

**On Windows (Command Prompt):**
```cmd
.venv\Scripts\activate.bat
```

**On Windows (PowerShell):**
```powershell
.venv\Scripts\Activate.ps1
```

### 3. Install dependencies

```bash
pip install -r requirements.txt
```

### 4. Set up environment variables

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

Open `.env` in your text editor and ensure you provide values for at least:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_KEY`

### 5. Run the server

```bash
python -m app.main
```

for uv
```bash
uv run python -m app.main
```

The service listens on `http://192.168.2.26:8000`. Interactive API documentation
is available at `http://192.168.2.26:8000/docs`.

## Endpoints

- `GET /health` checks service availability.
- `POST /api/v1/analyze` accepts an image in the `image` multipart field.

The analysis endpoint currently validates the upload and returns a stable
placeholder response until the face recognition model is connected.
