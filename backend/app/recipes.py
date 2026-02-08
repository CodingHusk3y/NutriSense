import os
import json
from typing import List, Dict, Any, Optional
from fastapi import APIRouter
from pydantic import BaseModel
import google.generativeai as genai
from .config import GEMINI_API_KEY

router = APIRouter()

# --- 1. Define the Request Model ---
# This matches the JSON sent by script.js:
# { "ingredients": [{"name": "Milk", ...}], "diet_type": "Vegan" }
class RecipeRequest(BaseModel):
    ingredients: List[Dict[str, Any]]  # Accepts the full ingredient objects
    diet_type: Optional[str] = "balanced"

# --- 2. The Endpoint ---
@router.post("/recipes/recommend")
def recommend_recipes(payload: RecipeRequest):
    try:
        # Extract just the names from the complex ingredient objects
        ingredient_names = [item.get("name", "") for item in payload.ingredients]
        # Filter out empty names
        valid_ingredients = [name for name in ingredient_names if name]

        if not valid_ingredients:
            return {"recipes": []}

        # If Gemini API key is missing, return fallback immediately
        if not GEMINI_API_KEY:
            return _fallback_recipes(valid_ingredients, payload.diet_type)

        # --- Call Gemini AI ---
        model = genai.GenerativeModel('gemini-2.5-flash-lite')
        
        prompt = f"""
        Suggest 3 healthy recipes using these ingredients: {", ".join(valid_ingredients)}.
        Dietary preference: {payload.diet_type}.
        
        Return ONLY valid JSON. 
        Format:
        {{
            "recipes": [
                {{
                    "title": "Recipe Name",
                    "reasoning": "Why this fits the ingredients",
                    "missing_ingredients": ["Item 1", "Item 2"],
                    "match_score": 85,
                    "macros": {{ "calories": 500, "protein": "30g", "carbs": "40g", "fats": "15g" }}
                }}
            ]
        }}
        """

        response = model.generate_content(prompt)
        
        # Clean up JSON safely (remove code fences if present)
        text = (getattr(response, "text", "") or "").strip()
        if "```json" in text:
            text = text.split("```json", 1)[1].split("```", 1)[0]
        else:
            text = text.replace("```", "")
        text = text.strip()

        try:
            data = json.loads(text)
        except Exception:
            data = _fallback_recipes(valid_ingredients, payload.diet_type)
        
        return data

    except Exception as e:
        # Graceful fallback on quota/rate-limit or any Gemini errors
        msg = str(e).lower()
        if "quota" in msg or "429" in msg or "rate" in msg:
            return _fallback_recipes(valid_ingredients if 'valid_ingredients' in locals() else [], payload.diet_type)
        # Generic fallback to avoid 500s
        return _fallback_recipes(valid_ingredients if 'valid_ingredients' in locals() else [], payload.diet_type)

def _fallback_recipes(ingredients: List[str], diet_type: Optional[str]) -> Dict[str, Any]:
    """Deterministic recipe suggestions when Gemini is unavailable or rate-limited."""
    base_title = "Healthy Bowl"
    if diet_type and diet_type.lower() == "vegan":
        base_title = "Plant-Powered Bowl"
    elif diet_type and diet_type.lower() == "keto":
        base_title = "Keto-Friendly Plate"

    common_miss = ["olive oil", "garlic", "salt", "pepper"]
    ing_lower = [x.lower() for x in ingredients]

    def mk(title_suffix: str, score_offset: int) -> Dict[str, Any]:
        return {
            "title": f"{base_title} with {title_suffix}",
            "reasoning": f"Uses available ingredients: {', '.join(ingredients)}",
            "missing_ingredients": [mi for mi in common_miss if mi not in ing_lower],
            "match_score": max(50, min(95, 70 + score_offset)),
            "macros": {"calories": 450, "protein": "25g", "carbs": "40g", "fats": "15g"}
        }

    top = ingredients[:3] or ["mixed veggies"]
    suffixes = [
        ", ".join(top),
        f"{top[0]} & grains" if top else "grains",
        f"{top[0]} & greens" if top else "greens",
    ]
    return {"recipes": [mk(suffixes[0], 10), mk(suffixes[1], 5), mk(suffixes[2], 0)]}