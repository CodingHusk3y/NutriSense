package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.supabase.*;
import com.nutrisense.nutritionengine.supabase.SupabaseRestClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreCatalogService {

    private final SupabaseRestClient supabase;

    private List<StoreRow> storesCache = new ArrayList<>();
    private Map<UUID, Map<String, Double>> storePriceByFoodNameCache = new HashMap<>();
    private long cacheLoadedAtMs = 0;
    private final Object cacheLock = new Object();

    public StoreCatalogService(SupabaseRestClient supabase) {
        this.supabase = supabase;
    }

    public List<StoreRow> getStores() {
        ensureLoaded();
        return storesCache;
    }

    public double getPrice(UUID storeId, String foodNameLower) {
        ensureLoaded();
        Map<String, Double> m = storePriceByFoodNameCache.getOrDefault(storeId, Collections.emptyMap());
        Double p = m.get(foodNameLower);
        return p == null ? -1 : p;
    }

    private void ensureLoaded() {
        synchronized(cacheLock) {
            long now = System.currentTimeMillis();
            if (!storesCache.isEmpty() && (now - cacheLoadedAtMs) < 60_000) return;

            // 1) load stores
            List<StoreRow> stores = supabase.getList("stores?select=id,name,chain,address,lat,lng", StoreRow.class);
            if (stores == null) stores = Collections.emptyList();

            // 2) load foods id map (name -> id)
            List<FoodIdRow> foods = supabase.getList("foods?select=id,name", FoodIdRow.class);
            if (foods == null) foods = Collections.emptyList();

            Map<UUID, String> foodIdToName = new HashMap<>();
            for (FoodIdRow f : foods) {
                if (f.getId() != null && f.getName() != null) {
                    foodIdToName.put(f.getId(), f.getName().trim().toLowerCase());
                }
            }

            // 3) load store_prices
            List<StorePriceRow> prices = supabase.getList("store_prices?select=store_id,food_id,price_usd,unit", StorePriceRow.class);
            if (prices == null) prices = Collections.emptyList();

            Map<UUID, Map<String, Double>> priceMap = new HashMap<>();
            for (StorePriceRow sp : prices) {
                if (sp.getStore_id() == null || sp.getFood_id() == null || sp.getPrice_usd() == null) continue;
                String foodName = foodIdToName.get(sp.getFood_id());
                if (foodName == null) continue;

                priceMap.computeIfAbsent(sp.getStore_id(), k -> new HashMap<>())
                        .put(foodName, sp.getPrice_usd());
            }

            storesCache = stores;
            storePriceByFoodNameCache = priceMap;
            cacheLoadedAtMs = now;

            System.out.println("StoreCatalog loaded stores=" + storesCache.size() + ", store_prices rows=" + prices.size());
        }
    }
}