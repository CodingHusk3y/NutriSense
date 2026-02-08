package com.nutrisense.nutritionengine.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodSuggestionRow {

    private String gap;
    private String reason;
    private Integer priority;

    @JsonProperty("diet_types")
    private List<String> dietTypes;

    // join: foods(name)
    private FoodJoin foods;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FoodJoin {
        private String name;
    }

    public String getFoodName() {
        return foods == null ? null : foods.getName();
    }
}