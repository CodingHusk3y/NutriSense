import json
import io
import requests
from typing import Optional, Dict, Any, List

from fastapi import FastAPI, HTTPException, UploadFile, File, Header
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from PIL import Image
import google.generativeai as genai
from pydantic import BaseModel

# Import shared config
from .config import (
    get_supabase_client, 
    get_bearer_token, 
    FRIDGE_BUCKET
)

# Import routers and logic
from .recipes import router as recipes_router
from .nutrition_logic import detect_gaps, calculate_targets
from .store_logic import recommend_stores

# --- Request Models ---
class AnalysisRequest(BaseModel):
    ingredients: List[Dict[str, Any]] # Expects [{"name": "Milk", "quantity": 1, ...}]
    user_location: Optional[Dict[str, float]] = None # {"lat": 10.0, "lng": 20.0}

class StoreRequest(BaseModel):
    lat: float
    lng: float
    items: List[str]

# --- App Setup ---
app = FastAPI(title="NutriSense Backend", version="0.1.0")

# Register the new recipe endpoints
app.include_router(recipes_router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "ok"}

# --- Fridge Scan ---
@app.post("/fridge/scan")
def scan_fridge(file: UploadFile = File(...), Authorization: Optional[str] = Header(default=None)):
    supabase = get_supabase_client()
    if supabase is None:
        raise HTTPException(status_code=500, detail="Supabase not configured")

    token = get_bearer_token(Authorization)
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")

    try:
        user_res = supabase.auth.get_user(token)
        user_id = user_res.user.id
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {e}")

    try:
        # Synchronous read
        content = file.file.read()
        path = f"{user_id}/{file.filename}"
        
        # Synchronous upload
        supabase.storage.from_(FRIDGE_BUCKET).upload(path, content, {
            "content-type": file.content_type or "application/octet-stream"
        })
    except Exception as e:
        print(f"Storage upload warning: {e}")

    try:
        model = genai.GenerativeModel('gemini-2.5-flash-lite')

        image = Image.open(io.BytesIO(content))

        prompt = """
        Analyze this fridge image. Return ONLY a JSON object with a key "items".
        Each item must have: "name", "quantity" (number), "unit", "freshness" (good/warning/expired), "confidence" (0-100).
        Example: {"items": [{"name": "Milk", "quantity": 1, "unit": "L", "freshness": "good", "confidence": 95}]}
        """

        response = model.generate_content([prompt, image])
        
        # Safer JSON parsing
        json_text = response.text
        if "```json" in json_text:
            json_text = json_text.split("```json")[1].split("```")[0]
        elif "```" in json_text:
            json_text = json_text.split("```")[0]
            
        result_data = json.loads(json_text.strip())

        for item in result_data.get("items", []):
            item["confirmed"] = False

        return result_data

    except Exception as e:
        print(f"Gemini error: {e}")
        return {"items": []}

# --- Profile Endpoints ---
@app.get("/profile")
def get_profile(Authorization: Optional[str] = Header(default=None)):
    supabase = get_supabase_client()
    if supabase is None:
        raise HTTPException(status_code=500, detail="Supabase not configured")

    token = get_bearer_token(Authorization)
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")

    try:
        user_res = supabase.auth.get_user(token)
        user_id = user_res.user.id

        res = supabase.table("profiles").select("*").eq("user_id", user_id).execute()
        data = res.data if hasattr(res, "data") else []
        if not data:
            return JSONResponse(status_code=404, content={"message": "Profile not found"})
        return data[0]
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Profile fetch failed: {e}")

@app.put("/profile")
def upsert_profile(payload: Dict[str, Any], Authorization: Optional[str] = Header(default=None)):
    supabase = get_supabase_client()
    if supabase is None:
        raise HTTPException(status_code=500, detail="Supabase not configured")

    token = get_bearer_token(Authorization)
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")

    try:
        user_res = supabase.auth.get_user(token)
        user_id = user_res.user.id

        allowed_keys = {"age", "gender", "weight_kg", "height_cm", "health_goal", "diet_type", "preferences"}
        safe_payload = {k: v for k, v in payload.items() if k in allowed_keys}

        existing = supabase.table("profiles").select("user_id").eq("user_id", user_id).execute()
        exists = bool(getattr(existing, "data", []))

        if exists:
            res = supabase.table("profiles").update(safe_payload).eq("user_id", user_id).execute()
        else:
            record = {"user_id": user_id, **safe_payload}
            res = supabase.table("profiles").insert(record).execute()

        return {"success": True, "data": getattr(res, "data", [])}
    except Exception as e:
        print(f"[profiles] upsert error: {e}")
        raise HTTPException(status_code=500, detail=f"Profile save failed: {e}")

# --- Nutrition Analysis (Python Native) ---
@app.post("/nutrition/analyze-fridge")
def analyze_fridge_nutrition(payload: AnalysisRequest, Authorization: Optional[str] = Header(default=None)):
    """
    1. Calculates User Targets
    2. Detects Nutritional Gaps based on Fridge
    3. Suggests Missing Items
    """
    supabase = get_supabase_client()
    token = get_bearer_token(Authorization)
    if not token: raise HTTPException(401, "Missing Token")

    # 1. Get Profile
    try:
        user = supabase.auth.get_user(token)
        res = supabase.table("profiles").select("*").eq("user_id", user.user.id).execute()
        profile = res.data[0] if res.data else {}
    except Exception:
        profile = {}

    # 2. Calculate Targets
    targets = calculate_targets(profile)

    # 3. Detect Gaps
    gaps = detect_gaps(profile, payload.ingredients)

    # 4. Generate Shopping List from Gaps
    shopping_list = []
    suggestions = []
    
    gap_map = {
        "LOW_PROTEIN": ("Chicken Breast", "Protein is low."),
        "NO_VEGGIES": ("Spinach", "No veggies detected."),
        "NO_FRUITS": ("Apples", "No fruits detected."),
        "LOW_FIBER": ("Oats", "Fiber is low."),
        "LOW_HEALTHY_FATS": ("Avocado", "Missing healthy fats.")
    }

    for gap in gaps:
        if gap in gap_map:
            item, reason = gap_map[gap]
            shopping_list.append(item)
            suggestions.append({"gap": gap, "suggestion": item, "reason": reason})

    return {
        "targets": targets,
        "gaps": gaps,
        "suggestions": suggestions,
        "shopping_list_generated": shopping_list
    }

@app.post("/stores/recommend")
def get_store_recommendations(payload: StoreRequest):
    """
    Returns top 3 stores based on price and distance for the given list.
    """
    results = recommend_stores(payload.lat, payload.lng, payload.items)
    return {"stores": results}