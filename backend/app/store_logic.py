import math
import time
import requests
from typing import List, Dict
from .config import get_supabase_client

# --- Caching ---
_STORE_CACHE = []
_PRICE_CACHE = {}
_STORE_LOAD_TIME = 0

def load_stores_data():
    global _STORE_CACHE, _PRICE_CACHE, _STORE_LOAD_TIME
    now = time.time()
    if _STORE_CACHE and (now - _STORE_LOAD_TIME < 60):
        return

    supabase = get_supabase_client()
    if not supabase: return

    try:
        # Stores
        s_res = supabase.table("stores").select("id,name,lat,lng").execute()
        _STORE_CACHE = s_res.data if s_res.data else []

        # Prices: Map StoreID -> { FoodName: Price }
        p_res = supabase.table("store_prices").select("store_id,food_id,price_usd").execute()
        f_res = supabase.table("foods").select("id,name").execute()
        
        # Create ID to Name Map
        id_to_name = {f["id"]: f["name"].lower() for f in (f_res.data or [])}
        
        new_prices = {}
        for row in (p_res.data or []):
            sid = row["store_id"]
            fid = row["food_id"]
            price = row["price_usd"]
            fname = id_to_name.get(fid)
            
            if sid and fname:
                if sid not in new_prices: new_prices[sid] = {}
                new_prices[sid][fname] = price
        
        _PRICE_CACHE = new_prices
        _STORE_LOAD_TIME = now
    except Exception as e:
        print(f"Store Load Error: {e}")

# --- Haversine Distance (Fallback if Google API fails) ---
def haversine_km(lat1, lon1, lat2, lon2):
    R = 6371  # Earth radius km
    dLat = math.radians(lat2 - lat1)
    dLon = math.radians(lon2 - lon1)
    a = math.sin(dLat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dLon/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return R * c

# --- Store Recommendation Logic ---
def recommend_stores(user_lat: float, user_lng: float, shopping_list: List[str]):
    load_stores_data()
    
    scored_stores = []
    missing_penalty = 6.00 # $6 penalty for missing item
    
    for store in _STORE_CACHE:
        store_id = store["id"]
        
        # 1. Calculate Total Price
        total_price = 0
        store_prices = _PRICE_CACHE.get(store_id, {})
        
        for item in shopping_list:
            price = store_prices.get(item.lower())
            if price is not None:
                total_price += price
            else:
                total_price += missing_penalty
        
        # 2. Calculate Distance
        # In production, call Google API here. For now, use Haversine.
        dist = haversine_km(user_lat, user_lng, store["lat"], store["lng"])
        
        # 3. Simple Score (Lower is better)
        # Weight: Price 50%, Distance 50% (Normalized roughly)
        score = (total_price * 1.0) + (dist * 2.0) # $1 ~= 2km driving cost heuristic
        
        scored_stores.append({
            "name": store["name"],
            "total_price": round(total_price, 2),
            "distance_km": round(dist, 1),
            "score": score
        })
        
    # Sort by best score
    scored_stores.sort(key=lambda x: x["score"])
    return scored_stores[:3] # Return top 3