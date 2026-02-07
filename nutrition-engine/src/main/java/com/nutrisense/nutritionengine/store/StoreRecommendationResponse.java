package com.nutrisense.nutritionengine.store;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StoreRecommendationResponse {
    private StoreScore bestOverall;
    private StoreScore cheapest;
    private StoreScore closest;
    private List<StoreScore> allStores;
}