# NutriSense – Nutrition Engine (Java Spring Boot)

This module is the **nutrition & ingredient recommendation engine** for the NutriSense hackathon project.  
It receives a user profile + detected fridge ingredients and returns **daily nutrition targets, food-group needs, gap detection, recommendations, and a smart shopping list**.

Built with **Java 21 + Spring Boot** and documented via **Swagger / OpenAPI**.

---

## What This Service Does

### Inputs
- **UserProfile**
    - age, gender, heightCm, weightKg
    - fitness goal (LOSE_WEIGHT / GAIN_MUSCLE / MAINTAIN)
    - diet type (BALANCED / VEGAN / VEGETARIAN / KETO / …)
    - **activityLevel** (SEDENTARY / LIGHT / MODERATE / ACTIVE / VERY_ACTIVE)
- **Ingredients**
    - name, quantity, unit
    - confidenceScore (from vision model)
    - optional freshness fields (purchaseDate, shelfLifeDays → FRESH/USE_SOON/EXPIRED)

### Outputs
- **NutritionTarget**
    - calories + macros (protein/carbs/fats)
    - **BMI** (informational)
- **FoodGroupTargets**
    - proteinGrams, carbsGrams, fatsGrams
    - veggieServings, fruitServings
    - fiberGrams
- **Gaps** (category-level)
    - e.g., LOW_PROTEIN, NO_VEGGIES, NO_FRUITS, LOW_FIBER, LOW_HEALTHY_FATS, LOW_COMPLEX_CARBS
- **Recommendations**
    - human-readable suggestions based on detected gaps + freshness warnings
- **ShoppingList**
    - item + reason
    - filtered by diet type (e.g., VEGAN → tofu/legumes instead of chicken)

---

## Core Logic

### 1) Profile → BMR → TDEE → Calorie Target
- Computes **BMR** (Mifflin-St Jeor)
- Applies **activityLevel** factor to estimate **TDEE**
- Adjusts calories by **goal** (cut/bulk/maintain)

### 2) Calories → Macros + Food Group Needs
- Converts calories → macro targets (protein/carbs/fats)
- Adds food-group targets (veg/fruit servings, fiber grams) using simple heuristics suitable for hackathon demo

### 3) Ingredient → Food Groups → Gap Detection
- Maps ingredients into food groups:
    - PROTEIN, VEGGIES, FRUITS, FIBER, CARBS, FATS
- Compares “what’s in the fridge” vs targets
- Detects gaps like:
    - LOW_PROTEIN, NO_VEGGIES, LOW_FIBER, etc.

### 4) Gap → Category Recommendations → Items
- Generates recommendations at the **category level** first (e.g., “Protein is low…”)
- Then generates a **shopping list** with concrete items + reasons
- Respects diet constraints (e.g., vegan/keto)

### 5) Freshness Awareness (Optional but supported)
- If purchaseDate + shelfLifeDays exist:
    - EXPIRED ingredients are ignored for “available inventory”
    - USE_SOON triggers “prioritize meals using them” messages

---

## Architecture (Microservice)

This engine is designed as a **separate service** from the computer vision backend.

| Component | Responsibility |
|----------|----------------|
| Vision Backend (Python) | Detect ingredients from fridge photos + confidence scores |
| **Nutrition Engine (this service)** | Targets, food groups, gaps, recommendations, shopping list |
| Frontend | Upload image, confirm ingredients, display outputs |

---

## API Endpoints

### Health Check

```
GET /api/nutrition/ping
```

### Main Nutrition Analysis
```
POST /api/nutrition/analyze
```

### Sample Request

```json
{
  "userProfile": {
    "age": 20,
    "weightKg": 60,
    "heightCm": 165,
    "gender": "female",
    "healthGoal": "LOSE_WEIGHT",
    "dietType": "BALANCED"
  },
  "ingredients": [
    { "name": "egg", "quantity": 2, "unit": "pieces", "confidenceScore": 0.92 }
  ]
}
```

### Sample Response

```json
{
  "nutritionTarget": {
    "calories": 1555.4,
    "protein": 72,
    "carbs": 219.6,
    "fats": 43.2
  },
  "recommendations": [
    "You may need more protein — consider chicken breast, tofu, or Greek yogurt.",
    "Add more low-calorie vegetables like broccoli or spinach."
  ],
  "shoppingList": [
    {
      "item": "Chicken breast",
      "reason": "Low current protein intake"
    }
  ]
}
```

---

## Tech Stack

- Java 21
- Spring Boot
- RESTful API
- Swagger / OpenAPI for documentation

---

## How to Run

```bash
cd nutrition-engine
mvn spring-boot:run
```

Swagger UI:
```
http://localhost:8080/swagger-ui.html
```