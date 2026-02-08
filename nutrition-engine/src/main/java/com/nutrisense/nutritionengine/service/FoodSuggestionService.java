package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.FoodGap;
import com.nutrisense.nutritionengine.supabase.FoodSuggestionRow;
import com.nutrisense.nutritionengine.supabase.SupabaseRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class FoodSuggestionService {

    private final SupabaseRestClient supabase;

    @Value("${supabase.suggestionsTable:food_suggestions}")
    private String suggestionsTable;

    public FoodSuggestionService(SupabaseRestClient supabase) {
        this.supabase = supabase;
    }

    public List<FoodSuggestionRow> findSuggestions(FoodGap gap, String dietType) {
        if (gap == null) return Collections.emptyList();

        // join foods(name)
        String q = suggestionsTable
                + "?gap=eq." + enc(gap.name())
                + "&select=gap,reason,priority,diet_types,foods(name)"
                + "&order=priority.asc";

        List<FoodSuggestionRow> rows = supabase.getList(q, FoodSuggestionRow.class);
        if (rows == null) rows = Collections.emptyList();

        String diet = (dietType == null) ? "" : dietType.trim().toUpperCase();

        List<FoodSuggestionRow> out = new ArrayList<>();
        for (FoodSuggestionRow r : rows) {
            // If no food name from join => skip
            if (r.getFoodName() == null || r.getFoodName().isBlank()) continue;

            List<String> diets = r.getDietTypes();
            if (diets == null || diets.isEmpty()) {
                out.add(r);
            } else {
                boolean ok = diets.stream()
                        .filter(Objects::nonNull)
                        .map(s -> s.trim().toUpperCase())
                        .anyMatch(s -> s.equals(diet));
                if (ok) out.add(r);
            }
        }
        return out;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}