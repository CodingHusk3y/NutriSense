package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.*;
import com.nutrisense.nutritionengine.supabase.FoodItemRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RecommendationService {

    private final NutritionService nutritionService;
    private final FoodGroupService foodGroupService;
    private final FoodCatalogService foodCatalogService;
    private final FoodSuggestionService foodSuggestionService;

    public RecommendationService(NutritionService nutritionService,
                                 FoodGroupService foodGroupService,
                                 FoodCatalogService foodCatalogService,
                                 FoodSuggestionService foodSuggestionService) {
        this.nutritionService = nutritionService;
        this.foodGroupService = foodGroupService;
        this.foodCatalogService = foodCatalogService;
        this.foodSuggestionService = foodSuggestionService;
    }

    public List<FoodGap> detectGaps(UserProfile user, List<Ingredient> ingredients) {
        // DB-only + unknown ingredient should fail fast
        validateIngredientsOrThrow(ingredients);

        NutritionTarget target = nutritionService.calculateTarget(user);
        Map<FoodGroup, Integer> counts = foodGroupService.countGroups(ingredients);

        // DB-based estimates
        double estimatedProtein = estimateProteinFromFridge(ingredients);

        List<FoodGap> gaps = new ArrayList<>();

        // thresholds
        if (estimatedProtein < target.getProtein() * 0.6) gaps.add(FoodGap.LOW_PROTEIN);

        if (counts.get(FoodGroup.VEGGIES) == 0) gaps.add(FoodGap.NO_VEGGIES);
        if (counts.get(FoodGroup.FRUITS) == 0) gaps.add(FoodGap.NO_FRUITS);

        if (counts.get(FoodGroup.FIBER) == 0) gaps.add(FoodGap.LOW_FIBER);

        // diet-specific soft gaps
        if ("KETO".equalsIgnoreCase(user.getDietType()) && counts.get(FoodGroup.FATS) == 0) {
            gaps.add(FoodGap.LOW_HEALTHY_FATS);
        }
        if (!"KETO".equalsIgnoreCase(user.getDietType()) && counts.get(FoodGroup.CARBS) == 0) {
            gaps.add(FoodGap.LOW_COMPLEX_CARBS);
        }

        return gaps;
    }

    public List<String> generateRecommendations(UserProfile user, List<Ingredient> ingredients, List<FoodGap> gaps) {
        List<String> recs = new ArrayList<>();
        if (gaps == null) gaps = Collections.emptyList();

        // freshness warnings
        long expired = ingredients == null ? 0 : ingredients.stream()
                .filter(i -> i != null && "EXPIRED".equalsIgnoreCase(i.getFreshnessStatus()))
                .count();
        long useSoon = ingredients == null ? 0 : ingredients.stream()
                .filter(i -> i != null && "USE_SOON".equalsIgnoreCase(i.getFreshnessStatus()))
                .count();

        if (expired > 0) recs.add("Some ingredients may be expired — please check and discard them for safety.");
        if (useSoon > 0) recs.add("You have ingredients to use soon — prioritize meals that use them to reduce waste.");

        for (FoodGap g : gaps) {
            switch (g) {
                case LOW_PROTEIN:
                    recs.add("Protein is low compared to your target — add a high-protein option today.");
                    break;
                case NO_VEGGIES:
                    recs.add("No vegetables detected — add low-calorie veggies for fiber and micronutrients.");
                    break;
                case NO_FRUITS:
                    recs.add("No fruits detected — add 1–2 servings for vitamins and fiber.");
                    break;
                case LOW_FIBER:
                    recs.add("Fiber looks low — add higher-fiber foods (e.g., oats, lentils, veggies, fruits).");
                    break;
                case LOW_HEALTHY_FATS:
                    recs.add("Healthy fats are missing — consider foods like avocado, olive oil, nuts, or salmon (diet permitting).");
                    break;
                case LOW_COMPLEX_CARBS:
                    recs.add("Complex carbs are missing — add whole grains or starchy carbs (diet permitting).");
                    break;
            }
        }
        return recs;
    }

    public List<ShoppingItem> generateShoppingList(UserProfile user, List<FoodGap> gaps) {
        if (gaps == null || gaps.isEmpty()) return Collections.emptyList();

        String diet = user.getDietType() == null ? "" : user.getDietType().trim().toUpperCase();

        List<ShoppingItem> list = new ArrayList<>();

        for (FoodGap g : gaps) {
            // fetch suggestions from DB
            var rows = foodSuggestionService.findSuggestions(g, diet);

            // pick top 2 per gap (tune as you want)
            int added = 0;
            for (var r : rows) {
                String foodName = r.getFoodName();
                if (foodName == null || foodName.isBlank()) continue;

                // ensure exists in foods (should always be true due to FK+join)
                if (foodCatalogService.find(foodName) == null) continue;

                list.add(new ShoppingItem(foodName, r.getReason()));
                added++;
                if (added >= 2) break;
            }
        }

        // de-dup by item name
        Map<String, ShoppingItem> uniq = new LinkedHashMap<>();
        for (ShoppingItem s : list) {
            if (s == null || s.getItem() == null) continue;
            uniq.putIfAbsent(s.getItem().trim().toLowerCase(), s);
        }

        return new ArrayList<>(uniq.values());
    }

    private double estimateProteinFromFridge(List<Ingredient> ingredients) {
        if (ingredients == null) return 0;

        double totalProtein = 0;

        for (Ingredient ing : ingredients) {
            if (ing == null || ing.getName() == null) continue;
            if ("EXPIRED".equalsIgnoreCase(ing.getFreshnessStatus())) continue;

            FoodItemRow row = foodCatalogService.find(ing.getName());
            if (row == null) {
                // validateIngredientsOrThrow should prevent this
                continue;
            }

            Double proteinPer100g = row.getProteinPer100g();
            if (proteinPer100g == null) proteinPer100g = 0.0;

            double grams = estimateGrams(ing); // you already have this helper
            totalProtein += proteinPer100g * (grams / 100.0);
        }

        return totalProtein;
    }

    private double estimateGrams(Ingredient ing) {
        if (ing.getUnit() == null || ing.getQuantity() <= 0) return 100.0;

        String unit = ing.getUnit().trim().toLowerCase();
        double qty = ing.getQuantity();

        switch (unit) {
            case "g":
            case "gram":
            case "grams":
                return qty;

            case "kg":
            case "kilogram":
            case "kilograms":
                return qty * 1000.0;

            case "ml":
                return qty;

            case "l":
                return qty * 1000.0;

            case "piece":
            case "pieces":
                return qty * 50.0;

            default:
                return 100.0;
        }
    }

    private void validateIngredientsOrThrow(List<Ingredient> ingredients) {
        if (ingredients == null) return;

        for (Ingredient ing : ingredients) {
            if (ing == null || ing.getName() == null) continue;

            String name = ing.getName().trim();
            if (name.isBlank()) continue;

            // DB-only unknown -> 400
            FoodItemRow row = foodCatalogService.find(name);
            if (row == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "UNKNOWN_INGREDIENT: '" + name + "' not found in foods table"
                );
            }
        }
    }
}