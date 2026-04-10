### Compiliation and Execution to Set up Backend

- Create .env file
    # Supabase Configuration
    SUPABASE_URL=...
    SUPABASE_SERVICE_ROLE_KEY=ey...

    # Google Gemini AI Key
    GEMINI_API_KEY=AI...

    # Storage Bucket Name
    FRIDGE_BUCKET=fridge

# Engine URL (Optional - usually fine as default)
NUTRITION_ENGINE_URL=http://127.0.0.1:8000

# Security and Monitoring
ALLOWED_ORIGINS=https://nutrisense.vercel.app,http://localhost:5500
ALLOWED_HOSTS=nutrisense-hu2j.onrender.com,localhost,127.0.0.1
ENABLE_HTTPS_REDIRECT=false
MONITOR_READ_KEY=change-me

- Create a New Virtual Environment
    python -m venv .venv

- Activate the Environment
    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
    .\.venv\Scripts\Activate

- Install All Libraries
    pip install fastapi uvicorn supabase python-dotenv pydantic Pillow google-generativeai python-multipart

- Run the Server
    uvicorn app.main:app --reload

## Security and Monitoring Endpoints

- `GET /health` - health probe
- `GET /metrics` - Prometheus metrics
- `GET /security/log-alerts` - recent suspicious request alerts (requires header `X-Monitor-Key`)

## Rate Limiting

Key endpoints are protected with request throttling, for example:
- `/fridge/scan`: 10 requests/minute
- `/profile` (write): 12 requests/minute
- `/nutrition/analyze-fridge`: 20 requests/minute
