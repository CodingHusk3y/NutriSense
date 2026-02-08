package com.nutrisense.nutritionengine.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StorePriceRow {
    private UUID store_id;
    private UUID food_id;
    private Double price_usd;
    private String unit;
}