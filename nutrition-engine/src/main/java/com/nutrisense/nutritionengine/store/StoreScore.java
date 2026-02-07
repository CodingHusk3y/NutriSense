package com.nutrisense.nutritionengine.store;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoreScore {
    private String storeId;
    private String storeName;

    private double totalPrice;
    private double distanceKm;

    private double normalizedPrice;
    private double normalizedDistance;

    // lower is better
    private double score;
}