package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.model.FoodGroupTargets;
import com.nutrisense.nutritionengine.model.NutritionTarget;
import com.nutrisense.nutritionengine.model.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class NutritionService {

    public NutritionTarget calculateTarget(UserProfile user) {
        double bmr;

        if ("male".equalsIgnoreCase(user.getGender())) {
            bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * user.getAge() + 5;
        } else {
            bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * user.getAge() - 161;
        }

        double activityFactor = mapActivityFactor(user.getActivityLevel());
        double tdee = bmr * activityFactor;

        String goal = user.getHealthGoal() == null ? "MAINTAIN" : user.getHealthGoal();
        switch (goal) {
            case "LOSE_WEIGHT": tdee -= 500; break;
            case "GAIN_MUSCLE": tdee += 300; break;
        }

        double protein = user.getWeightKg() * ("GAIN_MUSCLE".equals(goal) ? 2.0 : 1.2);
        double fats = (tdee * 0.25) / 9.0;
        double carbs = (tdee - (protein * 4.0 + fats * 9.0)) / 4.0;

        // BMI calculation
        double heightMeters = user.getHeightCm() / 100.0;
        double bmi = user.getWeightKg() / (heightMeters * heightMeters);

        return new NutritionTarget(
                round1(tdee),
                round1(protein),
                round1(carbs),
                round1(fats),
                round1(bmi)
        );
    }


    public FoodGroupTargets calculateFoodGroupTargets(UserProfile user) {
        NutritionTarget t = calculateTarget(user);

        // Simple heuristics for servings & fiber target:
        // - Veggies: 3–5 servings/day, more if goal is LOSE_WEIGHT (volume eating)
        // - Fruits: 1–3 servings/day
        // - Fiber: 25–38g/day (use weight/goal heuristic)
        int veggies = "LOSE_WEIGHT".equals(user.getHealthGoal()) ? 5 : 3;
        int fruits = 2;

        double fiber = ("male".equalsIgnoreCase(user.getGender())) ? 30 : 25;
        if ("LOSE_WEIGHT".equals(user.getHealthGoal())) fiber += 5;

        return new FoodGroupTargets(
                t.getProtein(),
                veggies,
                fruits,
                round1(fiber),
                t.getCarbs(),
                t.getFats()
        );
    }

    private double mapActivityFactor(String level) {
        if (level == null) return 1.4; // safe default
        switch (level.toUpperCase()) {
            case "SEDENTARY": return 1.2;
            case "LIGHT": return 1.375;
            case "MODERATE": return 1.55;
            case "ACTIVE": return 1.725;
            case "VERY_ACTIVE": return 1.9;
            default: return 1.4;
        }
    }

    private double round1(double x) {
        return Math.round(x * 10.0) / 10.0;
    }
}