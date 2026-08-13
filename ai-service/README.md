# AI Service

FastAPI service for image analysis in the attendance system.

## Run locally

```bash
cd ai-service
python -m venv .venv
```

Activate the virtual environment, then install dependencies:

```bash
python -m pip install -r requirements.txt
python -m app.main
```

The service listens on `http://localhost:8000`. Interactive API documentation is
available at `http://localhost:8000/docs`.

## Endpoints

- `GET /health` checks service availability.
- `POST /api/v1/analyze` accepts an image in the `image` multipart field.

The analysis endpoint currently validates the upload and returns a stable
placeholder response until the face recognition model is connected.
