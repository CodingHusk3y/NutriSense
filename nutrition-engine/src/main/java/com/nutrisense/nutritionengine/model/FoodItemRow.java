package com.nutrisense.nutritionengine.model;

import lombok.Data;
import java.util.List;

@Data
public class FoodItemRow {
    private String name;
    private Double protein;
    private Double carbs;
    private Double fats;
    private List<String> groups; // e.g. ["PROTEIN","VEGGIES"]
}