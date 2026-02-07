package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.FoodGroup;
import com.nutrisense.nutritionengine.model.Ingredient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodGroupService {

    // Hackathon mapping: ingredient name -> food groups
    // You can expand over time / load from JSON later.
    private static final Map<String, Set<FoodGroup>> GROUP_MAP = new HashMap<>();

    static {
        put("egg", FoodGroup.PROTEIN, FoodGroup.FATS);
        put("chicken breast", FoodGroup.PROTEIN);
        put("tofu", FoodGroup.PROTEIN);
        put("greek yogurt", FoodGroup.PROTEIN);

        put("spinach", FoodGroup.VEGGIES, FoodGroup.FIBER);
        put("broccoli", FoodGroup.VEGGIES, FoodGroup.FIBER);
        put("carrot", FoodGroup.VEGGIES, FoodGroup.FIBER);

        put("apple", FoodGroup.FRUITS, FoodGroup.FIBER);
        put("banana", FoodGroup.FRUITS, FoodGroup.CARBS);

        put("oats", FoodGroup.CARBS, FoodGroup.FIBER);
        put("rice", FoodGroup.CARBS);
        put("bread", FoodGroup.CARBS);

        put("avocado", FoodGroup.FATS, FoodGroup.FIBER);
        put("olive oil", FoodGroup.FATS);
        put("nuts", FoodGroup.FATS, FoodGroup.FIBER);
    }

    private static void put(String name, FoodGroup... groups) {
        GROUP_MAP.put(name.toLowerCase(), EnumSet.copyOf(Arrays.asList(groups)));
    }

    public Set<FoodGroup> groupsFor(String ingredientName) {
        if (ingredientName == null) return Collections.emptySet();
        return GROUP_MAP.getOrDefault(ingredientName.toLowerCase(), Collections.emptySet());
    }

    public Map<FoodGroup, Integer> countGroups(List<Ingredient> ingredients) {
        Map<FoodGroup, Integer> counts = new EnumMap<>(FoodGroup.class);
        for (FoodGroup g : FoodGroup.values()) counts.put(g, 0);

        if (ingredients == null) return counts;

        for (Ingredient ing : ingredients) {
            if (ing == null || ing.getName() == null) continue;
            // If expired, do not count as "available"
            if ("EXPIRED".equals(ing.getFreshnessStatus())) continue;

            Set<FoodGroup> groups = groupsFor(ing.getName());
            for (FoodGroup g : groups) {
                counts.put(g, counts.get(g) + 1);
            }
        }
        return counts;
    }
}