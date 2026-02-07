package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationService {

    private final NutritionService nutritionService;
    private final FoodGroupService foodGroupService;

    public RecommendationService(NutritionService nutritionService, FoodGroupService foodGroupService) {
        this.nutritionService = nutritionService;
        this.foodGroupService = foodGroupService;
    }

    public List<FoodGap> detectGaps(UserProfile user, List<Ingredient> ingredients) {
        NutritionTarget target = nutritionService.calculateTarget(user);
        Map<FoodGroup, Integer> counts = foodGroupService.countGroups(ingredients);

        // quick & stable hackathon estimates
        double estimatedProtein = estimateProteinFromFridge(ingredients);

        List<FoodGap> gaps = new ArrayList<>();

        // thresholds (stable for demo)
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

        // freshness warnings
        long expired = ingredients == null ? 0 : ingredients.stream()
                .filter(i -> i != null && "EXPIRED".equals(i.getFreshnessStatus()))
                .count();
        long useSoon = ingredients == null ? 0 : ingredients.stream()
                .filter(i -> i != null && "USE_SOON".equals(i.getFreshnessStatus()))
                .count();

        if (expired > 0) recs.add("Some ingredients may be expired — please check and discard them for safety.");
        if (useSoon > 0) recs.add("You have ingredients to use soon — prioritize meals that use them to reduce waste.");

        // category-level recommendations (matches your logic)
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
                    recs.add("Fiber looks low — add beans, oats, berries, or leafy greens.");
                    break;
                case LOW_HEALTHY_FATS:
                    recs.add("Healthy fats are missing — add avocado, olive oil, nuts, or salmon (diet permitting).");
                    break;
                case LOW_COMPLEX_CARBS:
                    recs.add("Complex carbs are missing — add oats, brown rice, or whole grains (diet permitting).");
                    break;
            }
        }

        return recs;
    }

    public List<ShoppingItem> generateShoppingList(UserProfile user, List<FoodGap> gaps) {
        List<ShoppingItem> list = new ArrayList<>();

        String diet = user.getDietType() == null ? "BALANCED" : user.getDietType().toUpperCase();
        boolean vegan = "VEGAN".equals(diet) || "VEGETARIAN".equals(diet);
        boolean keto = "KETO".equals(diet);

        for (FoodGap g : gaps) {
            switch (g) {
                case LOW_PROTEIN:
                    if (vegan) {
                        list.add(new ShoppingItem("Tofu", "Plant-based high protein"));
                        list.add(new ShoppingItem("Chickpeas", "Plant protein + fiber"));
                    } else {
                        list.add(new ShoppingItem("Chicken breast", "Low current protein intake"));
                        list.add(new ShoppingItem("Greek yogurt", "Good high-protein snack"));
                    }
                    break;

                case NO_VEGGIES:
                    list.add(new ShoppingItem("Broccoli", "Add vegetables for fiber and micronutrients"));
                    list.add(new ShoppingItem("Spinach", "Easy to add to meals, high micronutrients"));
                    break;

                case NO_FRUITS:
                    list.add(new ShoppingItem("Apples", "Convenient fruit serving"));
                    list.add(new ShoppingItem("Berries", "High fiber fruit option"));
                    break;

                case LOW_FIBER:
                    list.add(new ShoppingItem("Oats", "Easy fiber + complex carbs"));
                    list.add(new ShoppingItem("Beans/Lentils", "High fiber + protein"));
                    break;

                case LOW_HEALTHY_FATS:
                    list.add(new ShoppingItem("Avocado", "Healthy fats"));
                    list.add(new ShoppingItem("Olive oil", "Healthy fats for cooking"));
                    break;

                case LOW_COMPLEX_CARBS:
                    if (!keto) {
                        list.add(new ShoppingItem("Brown rice", "Complex carbs for energy"));
                        list.add(new ShoppingItem("Whole-grain bread", "Complex carbs"));
                    }
                    break;
            }
        }

        // de-dup by item name
        Map<String, ShoppingItem> uniq = new LinkedHashMap<>();
        for (ShoppingItem s : list) uniq.putIfAbsent(s.getItem().toLowerCase(), s);

        return new ArrayList<>(uniq.values());
    }

    private double estimateProteinFromFridge(List<Ingredient> ingredients) {
        if (ingredients == null) return 0;

        // quick hackathon: use a small lookup map (per "serving")
        Map<String, Double> proteinPerItem = Map.of(
                "egg", 6.0,
                "chicken breast", 31.0,
                "tofu", 10.0,
                "greek yogurt", 17.0,
                "milk", 8.0,
                "beans", 15.0,
                "lentils", 18.0
        );

        double total = 0;
        for (Ingredient ing : ingredients) {
            if (ing == null || ing.getName() == null) continue;
            if ("EXPIRED".equals(ing.getFreshnessStatus())) continue;

            Double p = proteinPerItem.get(ing.getName().toLowerCase());
            if (p != null) total += p;
        }
        return total;
    }
}