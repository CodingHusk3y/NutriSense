package com.nutrisense.nutritionengine.supabase;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodItemRow {

    private String name;

    @JsonProperty("protein_per_100g")
    private double proteinPer100g;

    @JsonProperty("carbs_per_100g")
    private double carbsPer100g;

    @JsonProperty("fats_per_100g")
    private double fatsPer100g;

    @JsonProperty("fiber_per_100g")
    private double fiberPer100g;

    @JsonProperty("calories_per_100g")
    private double caloriesPer100g;

    @JsonProperty("food_group")
    private String foodGroup;

    @JsonProperty("diet_tags")
    private List<String> dietTags;
}
