package com.nutrisense.nutritionengine.store;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Store {
    private String id;
    private String name;
    private double latitude;
    private double longitude;

    // ingredientName -> price (USD)
    private Map<String, Double> itemPrices;
}