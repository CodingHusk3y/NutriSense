package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.FoodGroup;
import com.nutrisense.nutritionengine.supabase.FoodItemRow;
import com.nutrisense.nutritionengine.model.Ingredient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodGroupService {

    // fallback mapping
    //private static final Map<String, Set<FoodGroup>> GROUP_MAP = new HashMap<>();

    private final FoodCatalogService foodCatalogService;

    public FoodGroupService(FoodCatalogService foodCatalogService) {
        this.foodCatalogService = foodCatalogService;
    }

    /*
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
    */

    public Set<FoodGroup> groupsFor(String ingredientName) {
        if (ingredientName == null) return Collections.emptySet();

        FoodItemRow row = foodCatalogService.find(ingredientName);
        if (row == null) return Collections.emptySet();

        EnumSet<FoodGroup> groups = EnumSet.noneOf(FoodGroup.class);

        FoodGroup primary = mapFoodGroup(row.getFoodGroup());
        if (primary != null) groups.add(primary);

        Double fiber = row.getFiberPer100g();
        if (fiber != null && fiber > 0) groups.add(FoodGroup.FIBER);

        return groups;
    }

    private FoodGroup mapFoodGroup(String foodGroup) {
        if (foodGroup == null) return null;
        switch (foodGroup.trim().toLowerCase()) {
            case "protein": return FoodGroup.PROTEIN;
            case "carb":
            case "carbs": return FoodGroup.CARBS;
            case "fat":
            case "fats": return FoodGroup.FATS;
            case "veggie":
            case "vegetable":
            case "vegetables": return FoodGroup.VEGGIES;
            case "fruit":
            case "fruits": return FoodGroup.FRUITS;
            default: return null;
        }
    }

    public Map<FoodGroup, Integer> countGroups(List<Ingredient> ingredients) {
        Map<FoodGroup, Integer> counts = new EnumMap<>(FoodGroup.class);
        for (FoodGroup g : FoodGroup.values()) counts.put(g, 0);

        if (ingredients == null) return counts;

        for (Ingredient ing : ingredients) {
            if (ing == null || ing.getName() == null) continue;
            if ("EXPIRED".equalsIgnoreCase(ing.getFreshnessStatus())) continue;

            Set<FoodGroup> groups = groupsFor(ing.getName());
            for (FoodGroup g : groups) counts.put(g, counts.get(g) + 1);
        }
        return counts;
    }
}