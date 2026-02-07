package com.nutrisense.nutritionengine.model;

import lombok.Data;
import java.util.List;

@Data
public class NutritionRequest {
    private UserProfile userProfile;
    private List<Ingredient> ingredients;
}