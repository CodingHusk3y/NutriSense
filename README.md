# NutriSense

## Overview

NutriSense is an intelligent food assistance system that integrates **computer vision, personalized nutrition logic, and recommendation algorithms** to help users make informed decisions about meals and grocery shopping. By analyzing the contents of a user’s fridge alongside personal health information and taste preferences, the system recommends missing ingredients, suitable recipes, and optimal stores for purchasing groceries.

The program is designed to be modular, transparent, and adaptable, with a strong emphasis on explainable recommendations rather than opaque decision-making.

---

## Core Functionality

### Fridge Ingredient Recognition

The system allows users to scan or upload an image of their fridge. A computer vision model detects common food ingredients and returns a list of identified items along with confidence scores. Users can review, confirm, add, or remove detected ingredients to ensure accuracy.

This step establishes the system’s understanding of the user’s current food inventory.

---

### User Profile and Personalization

NutriSense maintains a user profile containing basic demographic and lifestyle information, including age, gender, height, weight, activity level, and personal dietary goals. Users may also specify taste preferences through keywords or preferred cuisines.

Whenever profile information is updated, the system automatically recalculates nutrition targets and adjusts all recommendations accordingly.

---

### Nutrition Analysis and Ingredient Recommendations

Based on the user profile, the system estimates daily energy needs using standard metabolic formulas and translates these needs into high-level food group targets (e.g., protein, vegetables, carbohydrates, fats).

The detected fridge ingredients are mapped to these food groups. Any imbalance or missing categories are identified, and the system recommends additional ingredients to support a more balanced and healthy diet. These recommendations are intended for general wellness guidance and are not medical advice.

---

### Recipe Recommendation Engine

The recipe engine generates meal suggestions using both existing fridge ingredients and newly recommended items. Recipes are filtered and ranked using a combination of:

* Ingredient overlap
* Alignment with dietary goals
* Similarity to user taste preferences and cuisines

This approach ensures that suggested meals are practical, nutritionally appropriate, and aligned with the user’s personal tastes.

---

### Store Recommendation System

For ingredients that are missing, the system recommends nearby stores where users can purchase them. Each store is evaluated based on distance and total ingredient cost using a normalized weighted scoring function:

```
Store Score = (0.5 × normalized price) + (0.5 × normalized distance)
```

Lower scores indicate more favorable options. This method provides a transparent and easily adjustable way to balance cost and convenience.

---

## System Architecture

NutriSense follows a service-oriented architecture consisting of:

* A frontend interface for image capture, profile management, and result visualization
* A backend API responsible for orchestration and business logic
* Dedicated modules for vision processing, nutrition analysis, recipe recommendation, and store ranking
* External data sources for food recognition, nutrition reference data, recipes, and location-based information

Each module operates independently, allowing for flexibility, scalability, and future extensions.

---

## Adaptability and Extensibility

The system is designed to support continuous refinement. Ingredient detection models, nutrition rules, recipe datasets, and store-ranking parameters can be updated or replaced without affecting the overall workflow. This modular design enables the program to evolve alongside user needs and data availability.

---

## Disclaimer

NutriSense provides general food and nutrition recommendations for informational purposes only. It is not intended to diagnose, treat, or replace professional medical or dietary advice.

---

## Purpose of the Project

Help users make healthier, budget-aware decisions by:
- Scanning fridge images to detect available ingredients
- Personalizing nutrition targets from a simple user profile
- Recommending missing items to balance macros/food groups
- Suggesting practical recipes aligned to preferences
- Ranking nearby stores by distance and total basket price

---

## Tools Utilized

- Backend: **FastAPI**, **Uvicorn**, **Pydantic**, **httpx**, **Pillow**
- Data/Auth: **Supabase** (auth, storage, tables)
- AI: **Google Gemini** (`google-generativeai`, migrating to `google.genai`)
- Maps: **Google Distance Matrix API** (distance by mode; Haversine fallback)
- Frontend: **HTML/CSS (Inter font)**, **Vanilla JS**
- Java Service (optional): **Spring Boot 3**, **Springdoc OpenAPI**, **Maven**, **Eclipse Temurin 21**

---

## Challenges and How We Overcame Them

- Gemini API quota (429) caused recipe endpoint failures
	- Added a deterministic fallback so `/recipes/recommend` always returns JSON
	- Hardened JSON parsing for fenced ````json` blocks

- Missing Google Maps API key led to distance failures
	- Logged key presence; fell back to Haversine distance when the API is unavailable
	- Documented `.env` setup for `GOOGLE_MAPS_API_KEY` and `GOOGLE_MAPS_MODE`

- Import error in backend due to missing walking helpers
	- Restored `estimate_walking_steps()` and `estimate_walking_calories()` in `store_logic.py`

- UX duplication for distance/steps
	- Simplified frontend card to show a single distance value and removed redundant trip display

---

## Credits (Frameworks, APIs, Libraries)

- **FastAPI** (MIT) — Web framework for Python APIs
- **Uvicorn** (BSD) — ASGI server
- **Pydantic** (MIT) — Data validation
- **httpx** (BSD) — HTTP client
- **Pillow** (HPND) — Image processing
- **Supabase** — Auth, storage, and Postgres backend-as-a-service
- **Google Gemini API** — Image/recipe generation (deprecated `google-generativeai`; migrating to `google.genai`)
- **Google Distance Matrix API** — Driving/walking distance estimates
- **Spring Boot 3 / Springdoc OpenAPI** — Java service framework and OpenAPI docs

