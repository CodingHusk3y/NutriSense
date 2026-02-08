# NutriSense – Nutrition & Store Recommendation Engine
**_Java 21 + Spring Boot_**

This service powers the nutrition intelligence and smart grocery planning for the **NutriSense project**.  
It analyzes a user’s health profile and fridge contents, then recommends what to eat and where to buy missing items using real nutrition data and store pricing.

---

## What This Service Does

### Nutrition Intelligence
Given a user profile and detected ingredients, the engine calculates:

- Daily calorie and macronutrient targets
- Food group needs (vegetables, fruits, fiber, etc.)
- Diet gaps based on what is missing
- Personalized nutrition recommendations
- A smart shopping list tailored to the user's diet

All nutrition data comes from a **Supabase database** (`foods` table). No hardcoded ingredient logic is used.

---

### Smart Store Recommendation

Once the shopping list is generated, the system:

- Looks up real food prices from store databases
- Uses **Google Maps Distance Matrix API** to compute driving distance
- Ranks stores based on:
  - Total grocery cost
  - Travel distance

The response includes:

- Best overall store
- Cheapest store
- Closest store
- Full store comparison list

---

## Core Logic

### 1. Profile → Calories & Macros

Uses the Mifflin-St Jeor Equation to compute BMR.

Steps:
1. Calculate **BMR (Basal Metabolic Rate)**
2. Multiply by activity factor → **TDEE**
3. Adjust calories based on goal (lose weight / maintain / gain muscle)

Then converts calories into daily targets for:
- Protein
- Carbohydrates
- Fats

---

### 2. Food Group Targets

Adds daily targets for:
- Vegetable servings
- Fruit servings
- Fiber grams

---

### 3. Ingredient Analysis (Database-Driven)

Every ingredient is matched against the Supabase `foods` table.

Fields used:
- protein_per_100g
- carbs_per_100g
- fats_per_100g
- fiber_per_100g
- calories_per_100g
- food_group
- diet_tags

If an ingredient is not found in the database, the API returns a **400 BAD REQUEST** with:
UNKNOWN_INGREDIENT

---

### 4. Gap Detection

The system detects category-level nutrition gaps:

- LOW_PROTEIN
- NO_VEGGIES
- NO_FRUITS
- LOW_FIBER
- LOW_HEALTHY_FATS
- LOW_COMPLEX_CARBS

---

### 5. Shopping List Generation (Database-Backed)

Shopping suggestions come directly from the foods database and are filtered by:

- Food group needed
- User diet type
- Nutritional relevance

---

### 6. Store Recommendation Engine

Store and pricing data are stored in **Supabase**.

| Table | Purpose |
|------|---------|
| foods | Master nutrition database |
| stores | Store locations with latitude/longitude |
| store_prices | Food price per store |

**Scoring formula:**
``` 
score = 0.5 × normalized_price + 0.5 × normalized_distance
```

Distance is retrieved using **Google Distance Matrix API**.  
If the API fails, the system falls back to Haversine distance.

## API Endpoints

### Health Check
```GET /api/nutrition/ping```


---

### Nutrition Analysis
```POST /api/nutrition/analyze```
#### Sample Request
```json
{
  "userProfile": {
    "age": 22,
    "weightKg": 65,
    "heightCm": 169,
    "gender": "male",
    "healthGoal": "GAIN_MUSCLE",
    "dietType": "BALANCED",
    "activityLevel": "MODERATE"
  },
  "ingredients": [
    { "name": "egg", "quantity": 2, "unit": "pieces", "confidenceScore": 0.92 }
  ]
}
```

### Store Recommendation

```POST /api/stores/recommend```
#### Sample Request
```json
{
  "lat": 33.954764,
  "lng": -83.375151,
  "neededItems": [
    "egg", "apple", "salmon", "avocado", "bread", "greek yogurt"
  ]
}
```

### Tech Stack
* Java 21

* Spring Boot

* Supabase (PostgreSQL + REST API)

* Google Maps Distance Matrix API

* Swagger / OpenAPI

### How to Run
```
cd nutrition-engine
mvn spring-boot:run
```

### Swagger UI:
```html
http://localhost:8080/swagger-ui.html
```

### Required Environment Variables
```
supabase.url=YOUR_SUPABASE_URL
supabase.serviceRoleKey=YOUR_SERVICE_ROLE_KEY
supabase.foodTable=foods

google.maps.apiKey=YOUR_GOOGLE_MAPS_API_KEY
google.maps.mode=driving
```

### Demo Coordinates
For realistic store distance results, use coordinates of **Miller Learning Center UGA**:
```
Latitude: 33.954764
Longitude: -83.375151
```