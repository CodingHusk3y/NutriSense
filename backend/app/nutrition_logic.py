import math
import time
from typing import List, Dict, Optional, Set
from .config import get_supabase_client

# --- Caching Global Variables ---
_FOOD_CACHE = {}
_FOOD_CACHE_TIME = 0
CACHE_TTL = 60  # seconds

# --- 1. Food Catalog Logic ---
def get_food_catalog():
    global _FOOD_CACHE, _FOOD_CACHE_TIME
    now = time.time()
    
    # Return cache if valid
    if _FOOD_CACHE and (now - _FOOD_CACHE_TIME < CACHE_TTL):
        return _FOOD_CACHE

    supabase = get_supabase_client()
    if not supabase:
        return {}

    try:
        # Fetch foods
        res = supabase.table("foods").select("name,protein_per_100g,carbs_per_100g,fats_per_100g,fiber_per_100g,food_group,diet_tags").execute()
        rows = res.data if res.data else []
        
        mapping = {}
        for r in rows:
            if r.get("name"):
                mapping[r["name"].lower().strip()] = r
        
        _FOOD_CACHE = mapping
        _FOOD_CACHE_TIME = now
        return mapping
    except Exception as e:
        print(f"Food Catalog Error: {e}")
        return {}

# --- 2. Nutrition Calculator (BMR/TDEE) ---
def calculate_targets(profile: dict) -> dict:
    weight = float(profile.get("weight_kg", 70))
    height = float(profile.get("height_cm", 170))
    age = int(profile.get("age", 25))
    gender = profile.get("gender", "female").lower()
    goal = profile.get("health_goal", "MAINTAIN")
    activity = profile.get("activity_level", "MODERATE")

    # Mifflin-St Jeor Equation
    if gender == "male":
        bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5
    else:
        bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161

    # Activity Multipliers
    activity_map = {
        "SEDENTARY": 1.2, "LIGHT": 1.375, "MODERATE": 1.55,
        "ACTIVE": 1.725, "VERY_ACTIVE": 1.9
    }
    tdee = bmr * activity_map.get(activity.upper(), 1.375)

    # Goal Adjustments
    if goal == "LOSE_WEIGHT": tdee -= 500
    elif goal == "GAIN_MUSCLE": tdee += 300
    elif goal == "INCREASE_ENERGY": tdee += 150

    # Macro Split
    if goal == "GAIN_MUSCLE":
        protein = weight * 2.0
    else:
        protein = weight * 1.2
        
    fats = (tdee * 0.25) / 9.0
    carbs = (tdee - ((protein * 4) + (fats * 9))) / 4.0

    return {
        "calories": round(tdee, 1),
        "protein": round(protein, 1),
        "carbs": round(carbs, 1),
        "fats": round(fats, 1)
    }

# --- 3. Gap Detection Logic ---
def detect_gaps(profile: dict, ingredients: List[dict]) -> List[str]:
    targets = calculate_targets(profile)
    catalog = get_food_catalog()
    
    current_protein = 0
    veggie_count = 0
    fruit_count = 0
    fiber_count = 0
    healthy_fats = 0
    
    for item in ingredients:
        name = item.get("name", "").lower()
        qty = item.get("quantity", 0)
        
        data = catalog.get(name)
        if not data: continue

        # Simple Gram Estimation (simplified)
        grams = qty * 100 if item.get("unit") in ["kg", "l"] else qty  # rough hack for demo
        
        # Sum Protein
        p_per_100 = data.get("protein_per_100g") or 0
        current_protein += (p_per_100 * (grams / 100))

        # Check Groups
        group = (data.get("food_group") or "").upper()
        if group == "VEGGIES": veggie_count += 1
        if group == "FRUITS": fruit_count += 1
        if group == "FATS": healthy_fats += 1
        
        if (data.get("fiber_per_100g") or 0) > 2:
            fiber_count += 1

    gaps = []
    
    # Threshold Logic
    if current_protein < (targets["protein"] * 0.3): # Assuming fridge is for 1-2 days
        gaps.append("LOW_PROTEIN")
    
    if veggie_count == 0: gaps.append("NO_VEGGIES")
    if fruit_count == 0: gaps.append("NO_FRUITS")
    if fiber_count == 0: gaps.append("LOW_FIBER")
    
    diet = profile.get("diet_type", "").upper()
    if diet == "KETO" and healthy_fats == 0:
        gaps.append("LOW_HEALTHY_FATS")

    return gaps