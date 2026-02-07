package com.nutrisense.nutritionengine.store;

import lombok.Data;

import java.util.List;

@Data
public class StoreRequest {
    private double lat;
    private double lng;
    private List<String> neededItems;
}