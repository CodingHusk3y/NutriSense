package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.FoodItemRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodCatalogService {

    private final SupabaseRestClient supabase;

    @Value("${supabase.foodTable}")
    private String foodTable;

    private Map<String, FoodItemRow> cache = new HashMap<>();
    private long cacheLoadedAtMs = 0;

    public FoodCatalogService(SupabaseRestClient supabase) {
        this.supabase = supabase;
    }

    public FoodItemRow find(String ingredientName) {
        if (ingredientName == null) return null;
        ensureLoaded();
        return cache.get(ingredientName.toLowerCase());
    }

    private void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (!cache.isEmpty() && (now - cacheLoadedAtMs) < 60_000) return;

        // select only needed fields
        String q = foodTable + "?select=name,protein,carbs,fats,groups";

        List<FoodItemRow> rows;
        try {
            rows = supabase.get(q)
                    .bodyToFlux(FoodItemRow.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            // Supabase not ready / RLS blocked / wrong table
            rows = Collections.emptyList();
        }

        Map<String, FoodItemRow> map = new HashMap<>();
        for (FoodItemRow r : rows) {
            if (r.getName() != null) map.put(r.getName().toLowerCase(), r);
        }

        cache = map;
        cacheLoadedAtMs = now;
    }
}