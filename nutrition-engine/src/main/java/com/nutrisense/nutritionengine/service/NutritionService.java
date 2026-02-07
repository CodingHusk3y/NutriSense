package com.nutrisense.nutritionengine.service;

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

        double tdee = bmr * 1.5; // hackathon default activity

        String goal = user.getHealthGoal() == null ? "MAINTAIN" : user.getHealthGoal();
        switch (goal) {
            case "LOSE_WEIGHT": tdee -= 500; break;
            case "GAIN_MUSCLE": tdee += 300; break;
            default: break;
        }

        double protein = user.getWeightKg() * ("GAIN_MUSCLE".equals(goal) ? 2.0 : 1.2);
        double fats = (tdee * 0.25) / 9.0;
        double carbs = (tdee - (protein * 4.0 + fats * 9.0)) / 4.0;

        return new NutritionTarget(round1(tdee), round1(protein), round1(carbs), round1(fats));
    }

    private double round1(double x) {
        return Math.round(x * 10.0) / 10.0;
    }
}