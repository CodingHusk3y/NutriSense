package com.nutrisense.nutritionengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NutritionTarget {
    private double calories;
    private double protein;
    private double carbs;
    private double fats;
    private double bmi;
}