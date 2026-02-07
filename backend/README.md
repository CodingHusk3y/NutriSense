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

- Create a New Virtual Environment
    python -m venv .venv

- Activate the Environment
    Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process
    .\.venv\Scripts\Activate

- Install All Libraries
    pip install fastapi uvicorn supabase python-dotenv pydantic Pillow google-generativeai python-multipart

- Run the Server
    uvicorn app.main:app --reload