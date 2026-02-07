import json
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Header
from pydantic import BaseModel
import google.generativeai as genai

# Import shared config
from .config import get_supabase_client, get_bearer_token

router = APIRouter()

# --- Data Models ---
class IngredientItem(BaseModel):
    name: str
    quantity: float | int
    unit: str

class RecipeRequest(BaseModel):
    ingredients: List[IngredientItem]
    diet_type: Optional[str] = None 
    meal_type: Optional[str] = "Dinner"

# --- Logic ---
@router.post("/recipes/recommend")
def recommend_recipes(payload: RecipeRequest, Authorization: Optional[str] = Header(default=None)):
    """
    Generates 3 recipes based on ingredients and user profile (allergies/goals).
    """
    # 1. Auth Check
    supabase = get_supabase_client()
    if not supabase:
        raise HTTPException(status_code=500, detail="Supabase not configured")
    
    token = get_bearer_token(Authorization)
    if not token:
        raise HTTPException(status_code=401, detail="Missing bearer token")

    # 2. Get User Context (Allergies/Goals)
    try:
        user_res = supabase.auth.get_user(token)
        user_id = user_res.user.id
        
        # Fetch profile
        res = supabase.table("profiles").select("*").eq("user_id", user_id).execute()
        profile = res.data[0] if res.data else {}
        
        # Determine context
        diet = payload.diet_type or profile.get("diet_type", "Any")
        goal = profile.get("health_goal", "General Health")
        allergies = profile.get("preferences", {}).get("allergies", [])
        
        user_context = f"Diet Preference: {diet}. Health Goal: {goal}. Allergies to avoid: {allergies}."
    except Exception as e:
        print(f"Profile fetch warning: {e}")
        user_context = "Diet: Any."

    # 3. Construct Prompt
    ingredients_str = ", ".join([f"{i.quantity} {i.unit} {i.name}" for i in payload.ingredients])
    
    prompt = f"""
    Act as a nutritionist/chef.
    User Context: {user_context}
    Ingredients Available: {ingredients_str}
    Meal Type: {payload.meal_type}

    Task: Suggest 3 recipes using these ingredients. You can assume basic pantry items (salt, oil, pepper).
    
    Return ONLY valid JSON in this exact format:
    {{
      "recipes": [
        {{
          "title": "Recipe Name",
          "match_score": 90,
          "missing_ingredients": ["item1"],
          "instructions": ["step 1", "step 2"],
          "macros": {{"calories": 500, "protein": "30g", "carbs": "40g", "fat": "20g"}},
          "reasoning": "Why this fits the user"
        }}
      ]
    }}
    """

    # 4. Call Gemini
    try:
        model = genai.GenerativeModel('gemini-2.5-flash-lite')

        # Temperature 0.4 keeps it creative but structured
        response = model.generate_content(prompt, generation_config={"temperature": 0.4})
        
        # 5. Clean & Parse JSON
        text = response.text
        if "```json" in text:
            text = text.split("```json")[1].split("```")[0]
        elif "```" in text:
            text = text.split("```")[0]
            
        return json.loads(text.strip())

    except Exception as e:
        print(f"Recipe generation error: {e}")
        raise HTTPException(status_code=500, detail="Failed to generate recipes")