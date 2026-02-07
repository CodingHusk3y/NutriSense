package com.nutrisense.nutritionengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class NutritionResponse {
    private NutritionTarget nutritionTarget;
    private List<String> recommendations;
    private List<ShoppingItem> shoppingList;
}