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