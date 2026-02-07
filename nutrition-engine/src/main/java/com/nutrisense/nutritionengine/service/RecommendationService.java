package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.Ingredient;
import com.nutrisense.nutritionengine.model.ShoppingItem;
import com.nutrisense.nutritionengine.model.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    public List<String> generateRecommendations(UserProfile user, List<Ingredient> ingredients) {
        List<String> recs = new ArrayList<>();

        double totalProtein = 0;
        int expiredCount = 0;
        int useSoonCount = 0;

        for (Ingredient ing : ingredients) {
            Double protein = FoodDatabase.PROTEIN_MAP.get(ing.getName().toLowerCase());
            if (protein != null) {
                totalProtein += protein;
            }

            String freshness = ing.getFreshnessStatus();
            if ("EXPIRED".equals(freshness)) expiredCount++;
            if ("USE_SOON".equals(freshness)) useSoonCount++;
        }

        // Protein gap
        if (totalProtein < 40) {
            recs.add("You may need more protein — consider chicken breast, tofu, or Greek yogurt.");
        }

        // Health goal
        if ("LOSE_WEIGHT".equals(user.getHealthGoal())) {
            recs.add("Add more low-calorie vegetables like broccoli or spinach.");
        }

        if ("GAIN_MUSCLE".equals(user.getHealthGoal())) {
            recs.add("Increase protein intake and add complex carbs like rice or oats.");
        }

        // Freshness logic
        if (expiredCount > 0) {
            recs.add("Some ingredients may be expired — check and discard them for safety.");
        }

        if (useSoonCount > 0) {
            recs.add("You have ingredients that should be used soon — prioritize meals using them.");
        }

        return recs;
    }


    public List<ShoppingItem> generateShoppingList(UserProfile user, List<Ingredient> ingredients) {
        List<ShoppingItem> list = new ArrayList<>();

        double totalProtein = 0;
        for (Ingredient ing : ingredients) {
            if ("EXPIRED".equals(ing.getFreshnessStatus())) continue; // bỏ qua đồ hỏng

            Double protein = FoodDatabase.PROTEIN_MAP.get(ing.getName().toLowerCase());
            if (protein != null) totalProtein += protein;
        }


        String diet = user.getDietType() == null ? "BALANCED" : user.getDietType().toUpperCase();

        boolean vegan = "VEGAN".equals(diet) || "VEGETARIAN".equals(diet);
        boolean keto = "KETO".equals(diet);

        if (totalProtein < 40) {
            if (vegan) {
                list.add(new ShoppingItem("Tofu", "Plant-based high protein"));
                list.add(new ShoppingItem("Chickpeas", "Plant-based protein + fiber"));
            } else {
                list.add(new ShoppingItem("Chicken breast", "Low current protein intake"));
                list.add(new ShoppingItem("Greek yogurt", "Good high-protein snack"));
            }
        }

        if (keto) {
            list.add(new ShoppingItem("Avocado", "Healthy fats for keto"));
            list.add(new ShoppingItem("Olive oil", "Healthy fats for keto"));
        } else {
            list.add(new ShoppingItem("Broccoli", "Add more vegetables for fiber and micronutrients"));
        }

        return list;
    }

}