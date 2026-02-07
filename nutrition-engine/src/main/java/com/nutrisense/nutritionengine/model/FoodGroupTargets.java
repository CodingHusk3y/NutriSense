package com.nutrisense.nutritionengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FoodGroupTargets {
    private double proteinGrams;     // per day
    private int veggieServings;      // per day (heuristic)
    private int fruitServings;       // per day (heuristic)
    private double fiberGrams;       // per day
    private double carbsGrams;       // per day
    private double fatsGrams;        // per day
}