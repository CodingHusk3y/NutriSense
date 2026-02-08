package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.supabase.FoodItemRow;
import com.nutrisense.nutritionengine.supabase.SupabaseRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodCatalogService {

    private final SupabaseRestClient supabase;

    @Value("${supabase.foodTable:foods}")
    private String foodTable;


    private Map<String, FoodItemRow> cache = new HashMap<>();
    private long cacheLoadedAtMs = 0;

    public FoodCatalogService(SupabaseRestClient supabase) {
        this.supabase = supabase;
    }

    public FoodItemRow find(String ingredientName) {
        if (ingredientName == null) return null;
        ensureLoaded();
        return cache.get(ingredientName.trim().toLowerCase());
    }

    private void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && (now - cacheLoadedAtMs) < 60_000) return;

        // foodTable = "food_catalog" (vd) => query: food_catalog?select=...
        String q = foodTable + "?select=name,protein_per_100g,carbs_per_100g,fats_per_100g,fiber_per_100g,calories_per_100g,food_group,diet_tags";
        System.out.println("FoodCatalog query table=" + foodTable);
        System.out.println("FoodCatalog query path=" + q);

        List<FoodItemRow> rows;
        try {
            rows = supabase.getList(q, FoodItemRow.class);
            if (rows == null) rows = Collections.emptyList();
        } catch (Exception e) {
            rows = Collections.emptyList();
        }

        System.out.println("FoodCatalog loaded rows=" + rows.size());

        Map<String, FoodItemRow> map = new HashMap<>();
        for (FoodItemRow r : rows) {
            if (r.getName() != null) map.put(r.getName().toLowerCase(), r);
        }

        cache = map;
        cacheLoadedAtMs = now;
    }
}