package com.nutrisense.nutritionengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShoppingItem {
    private String item;
    private String reason;
}