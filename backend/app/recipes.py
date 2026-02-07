import os
import json
from typing import List, Dict, Any, Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import google.generativeai as genai

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
        
        # Clean up JSON (remove backticks if Gemini adds them)
        text = response.text.replace("```json", "").replace("```", "").strip()
        data = json.loads(text)
        
        return data

    except Exception as e:
        print(f"Recipe Error: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate recipes")