package com.nutrisense.nutritionengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NutritionResponse {
    private NutritionTarget nutritionTarget;        // calories + macros
    private FoodGroupTargets foodGroupTargets;      // same info + veggie/fruit/fiber targets
    private List<FoodGap> gaps;                     // category gaps
    private List<String> recommendations;           // human-readable
    private List<ShoppingItem> shoppingList;        // items + reason
}