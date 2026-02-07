package com.nutrisense.nutritionengine.model;

import lombok.Data;

@Data
public class UserProfile {
    private int age;
    private double weightKg;
    private double heightCm;
    private String gender;      // "male" / "female"
    private String healthGoal;  // "LOSE_WEIGHT" / "GAIN_MUSCLE" / "MAINTAIN"
    private String dietType;    // "VEGAN" / "KETO" / "BALANCED" ...
    private String activityLevel;   // "Sedentary" / "Light" / "Moderate" / "Heavy" / "Athlete"
}