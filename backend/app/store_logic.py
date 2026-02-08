import math
import time
import os
import httpx
from typing import List, Dict, Optional, Any
from dataclasses import dataclass
from .config import get_supabase_client
print(f"DEBUG: API Key length is {len(os.getenv('GOOGLE_MAPS_API_KEY', ''))}")

# --- Configuration ---
# GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY", "")
GOOGLE_MAPS_MODE = os.getenv("GOOGLE_MAPS_MODE", "driving")

# --- Caching Globals ---
_STORE_CACHE: List[Dict] = []
_PRICE_CACHE: Dict[str, Dict[str, float]] = {}
_STORE_LOAD_TIME = 0
_CACHE_EXPIRY = 60  # seconds

# --- Constants ---
WEIGHT_PRICE = 0.5
WEIGHT_DISTANCE = 0.5
MISSING_ITEM_PENALTY_PRICE = 6.00

@dataclass
class StoreScore:
    id: str
    name: str
    chain: Optional[str]
    address: Optional[str]
    lat: float
    lng: float
    total_price: float
    distance_km: float
    norm_price: float
    norm_dist: float
    score: float

# --- 1. Google Distance Matrix Service ---
async def get_google_distance_km(from_lat: float, from_lng: float, to_lat: float, to_lng: float) -> float:
    # 1. Load Key HERE (Runtime), not at the top of the file
    api_key = os.getenv("GOOGLE_MAPS_API_KEY", "")
    mode = os.getenv("GOOGLE_MAPS_MODE", "driving")

    # 2. Debug Print: Check if key exists (Safe print, doesn't reveal full key)
    # This will print EVERY time the function runs
    if not api_key:
        print("❌ ERROR: Google Maps API Key is MISSING or EMPTY.")
        return -1.0
    
    # print(f"DEBUG: Using Key: {api_key[:4]}...******") # Uncomment if you want to verify the key loaded

    origins = f"{from_lat},{from_lng}"
    destinations = f"{to_lat},{to_lng}"
    url = "https://maps.googleapis.com/maps/api/distancematrix/json"

    params = {
        "origins": origins,
        "destinations": destinations,
        "mode": mode,
        "key": api_key
    }

    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, params=params)
            data = response.json()

        # 3. Print the REAL Error Message from Google
        if data.get("status") != "OK":
            error_msg = data.get("error_message", "No error message provided")
            print(f"⚠️ Google API Denied: {data.get('status')} - {error_msg}")
            return -1.0

        rows = data.get("rows", [])
        if not rows: return -1.0
        
        elements = rows[0].get("elements", [])
        if not elements: return -1.0
        
        element = elements[0]
        if element.get("status") != "OK":
            return -1.0

        distance_meters = element.get("distance", {}).get("value", 0)
        return distance_meters / 1000.0

    except Exception as e:
        print(f"Google DM Exception: {e}")
        return -1.0

# --- 2. Haversine Fallback ---
def haversine_km(lat1, lon1, lat2, lon2):
    try:
        R = 6371  # Earth radius km
        dLat = math.radians(lat2 - lat1)
        dLon = math.radians(lon2 - lon1)
        a = math.sin(dLat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dLon/2)**2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
        return R * c
    except:
        return 999.0

# --- 3. Data Loading ---
def load_stores_data():
    global _STORE_CACHE, _PRICE_CACHE, _STORE_LOAD_TIME
    now = time.time()
    if _STORE_CACHE and (now - _STORE_LOAD_TIME < _CACHE_EXPIRY):
        return

    supabase = get_supabase_client()
    if not supabase: return

    try:
        print("Fetching fresh store data...")
        # Java version fetches chain and address too
        s_res = supabase.table("stores").select("id,name,chain,address,lat,lng").execute()
        _STORE_CACHE = s_res.data if s_res.data else []

        # Prices
        p_res = supabase.table("store_prices").select("store_id,food_id,price_usd").execute()
        f_res = supabase.table("foods").select("id,name").execute()
        
        id_to_name = {f["id"]: f["name"].lower().strip() for f in (f_res.data or [])}
        
        new_prices = {}
        for row in (p_res.data or []):
            sid = row["store_id"]
            fid = row["food_id"]
            price = row["price_usd"]
            fname = id_to_name.get(fid)
            
            if sid and fname:
                if sid not in new_prices: new_prices[sid] = {}
                new_prices[sid][fname] = float(price)
        
        _PRICE_CACHE = new_prices
        _STORE_LOAD_TIME = now
    except Exception as e:
        print(f"Store Load Error: {e}")

# --- 4. Helpers for Normalization ---
def normalize(x: float, min_val: float, max_val: float) -> float:
    denom = max_val - min_val
    if denom == 0: return 0.0
    return (x - min_val) / denom

def calculate_store_total(store_id: str, items: List[str]) -> float:
    total = 0.0
    store_inventory = _PRICE_CACHE.get(store_id, {})
    
    for item in items:
        key = item.lower().strip()
        price = store_inventory.get(key)
        
        # If exact match fails, try partial match (simple fallback)
        if price is None:
            # Check if any inventory item contains our search term
            for inv_item, inv_price in store_inventory.items():
                if key in inv_item:
                    price = inv_price
                    break
        
        if price is not None:
            total += price
        else:
            total += MISSING_ITEM_PENALTY_PRICE
            
    return total

# --- 5. Main Recommendation Logic ---
async def find_stores_for_items(user_lat: float, user_lng: float, items: List[str]):
    load_stores_data()
    
    # 1. Calculate raw totals and distances for all stores
    temp_results = []
    prices_list = []
    dists_list = []
    
    for store in _STORE_CACHE:
        store_id = store.get("id")
        if not store_id: continue

        # Price Calculation
        total_price = calculate_store_total(store_id, items)
        
        # Distance Calculation (Try Google first, then Haversine)
        dist_km = await get_google_distance_km(user_lat, user_lng, store["lat"], store["lng"])
        if dist_km < 0:
            dist_km = haversine_km(user_lat, user_lng, store["lat"], store["lng"])
            
        temp_results.append({
            "store": store,
            "total_price": total_price,
            "distance_km": dist_km
        })
        
        prices_list.append(total_price)
        dists_list.append(dist_km)

    if not temp_results:
        return {"stores": [], "best": None, "cheapest": None, "closest": None}

    # 2. Determine Min/Max for Normalization
    min_price = min(prices_list) if prices_list else 0
    max_price = max(prices_list) if prices_list else 0
    min_dist = min(dists_list) if dists_list else 0
    max_dist = max(dists_list) if dists_list else 0

    # 3. Calculate Final Scores
    scored_stores: List[StoreScore] = []
    
    for res in temp_results:
        s = res["store"]
        p = res["total_price"]
        d = res["distance_km"]
        
        norm_p = normalize(p, min_price, max_price)
        norm_d = normalize(d, min_dist, max_dist)
        
        # Java logic: 0.5 * normPrice + 0.5 * normDist
        final_score = (WEIGHT_PRICE * norm_p) + (WEIGHT_DISTANCE * norm_d)
        
        scored_stores.append(StoreScore(
            id=s["id"],
            name=s["name"],
            chain=s.get("chain"),
            address=s.get("address"),
            lat=s["lat"],
            lng=s["lng"],
            total_price=round(p, 2),
            distance_km=round(d, 2),
            norm_price=round(norm_p, 3),
            norm_dist=round(norm_d, 3),
            score=round(final_score, 3)
        ))

    # Sort by score (lower is better)
    scored_stores.sort(key=lambda x: x.score)

    # 4. Identify Personas (Best, Cheapest, Closest)
    best_overall = scored_stores[0] if scored_stores else None
    
    # Cheapest: Sort by price
    cheapest = min(scored_stores, key=lambda x: x.total_price) if scored_stores else None
    
    # Closest: Sort by distance
    closest = min(scored_stores, key=lambda x: x.distance_km) if scored_stores else None

    # Format Output for Frontend
    # Return top 5 in the main list
    formatted_list = [
        {
            "id": s.id,
            "name": s.name,
            "chain": s.chain,
            "address": s.address,
            "total_price": s.total_price,
            "distance_km": s.distance_km,
            "score": s.score,
            "stock_status": "In Stock" # Placeholder, logic can be added later
        }
        for s in scored_stores[:5]
    ]

    return {
        "stores": formatted_list,
        "best_overall": {
            "name": best_overall.name,
            "total_price": best_overall.total_price,
            "distance_km": best_overall.distance_km
        } if best_overall else None,
        "cheapest": {
            "name": cheapest.name,
            "total_price": cheapest.total_price,
            "distance_km": cheapest.distance_km
        } if cheapest else None,
        "closest": {
            "name": closest.name,
            "total_price": closest.total_price,
            "distance_km": closest.distance_km
        } if closest else None
    }

# --- Walking Steps & Calories Estimation ---
def estimate_walking_steps(distance_km: float, gender: str = None) -> int:
    """
    Rough steps estimate based on average stride length.
    Defaults to ~0.75m per step (~1333 steps per km).
    """
    stride_m = 0.75
    if gender == "male":
        stride_m = 0.78
    elif gender == "female":
        stride_m = 0.70
    steps = (distance_km * 1000.0) / stride_m
    return max(0, int(round(steps)))

def estimate_walking_calories(distance_km: float, weight_kg: float = 70.0, speed_kmh: float = 4.8, met: float = 3.5) -> int:
    """
    Estimate calories using MET formula:
    calories = MET * weight_kg * hours, hours = distance_km / speed_kmh
    For 70kg at 1km, ~50-55 kcal.
    """
    try:
        hours = distance_km / speed_kmh if speed_kmh > 0 else 0
        calories = met * float(weight_kg or 70.0) * hours
        return max(0, int(round(calories)))
    except Exception:
        return 0