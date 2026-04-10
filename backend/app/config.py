import os
from typing import Optional, List, Any
from dotenv import load_dotenv
import google.generativeai as genai

# Supabase Client Imports
try:
    from supabase import create_client, Client
except ImportError:
    create_client = None
    Client = None

# Load environment variables
load_dotenv()

# Env Vars
SUPABASE_URL = os.getenv("SUPABASE_URL", "")
SUPABASE_SERVICE_ROLE_KEY = os.getenv("SUPABASE_SERVICE_ROLE_KEY", os.getenv("SUPABASE_ANON_KEY", ""))
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
FRIDGE_BUCKET = os.getenv("FRIDGE_BUCKET", "fridge")
NUTRITION_ENGINE_URL = os.getenv("NUTRITION_ENGINE_URL", "http://127.0.0.1:8000")
PRESAGE_API_KEY = os.getenv("PRESAGE_API_KEY")
PRESAGE_API_URL = os.getenv("PRESAGE_API_URL", "https://api.physiology.presagetech.com")


def _csv_env(name: str, default: str) -> List[str]:
    raw = os.getenv(name, default)
    return [item.strip() for item in raw.split(",") if item.strip()]


ALLOWED_ORIGINS = _csv_env(
    "ALLOWED_ORIGINS",
    "https://nutrisense.vercel.app,http://localhost:3000,http://127.0.0.1:5500,http://localhost:5500"
)
ALLOWED_HOSTS = _csv_env("ALLOWED_HOSTS", "*")
ENABLE_HTTPS_REDIRECT = os.getenv("ENABLE_HTTPS_REDIRECT", "false").lower() == "true"
MONITOR_READ_KEY = os.getenv("MONITOR_READ_KEY", "")

# Configure Gemini globally
if GEMINI_API_KEY:
    genai.configure(api_key=GEMINI_API_KEY)
else:
    print("WARNING: GEMINI_API_KEY not found in .env")

# Shared Helper Functions
def get_supabase_client() -> Optional[Any]:
    if not SUPABASE_URL or not SUPABASE_SERVICE_ROLE_KEY or create_client is None:
        return None
    return create_client(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

def get_bearer_token(auth_header: Optional[str]) -> Optional[str]:
    if not auth_header:
        return None
    parts = auth_header.split()
    if len(parts) == 2 and parts[0].lower() == "bearer":
        return parts[1]
    return None

def get_presage_headers() -> dict:
    """Returns authorization headers for Presage API if configured."""
    if not PRESAGE_API_KEY:
        return {}
    return {
        "Authorization": f"Bearer {PRESAGE_API_KEY}",
        "Content-Type": "application/json"
    }