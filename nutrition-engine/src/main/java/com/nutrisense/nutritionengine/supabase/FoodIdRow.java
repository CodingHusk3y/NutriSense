package com.nutrisense.nutritionengine.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoodIdRow {
    private UUID id;
    private String name;
}