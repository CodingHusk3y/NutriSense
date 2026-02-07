package com.nutrisense.nutritionengine.model;

import lombok.Data;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
public class Ingredient {
    private String name;
    private double quantity;        // optional
    private String unit;            // "g", "ml", "pieces"
    private LocalDate purchaseDate; // optional for hackathon
    private int shelfLifeDays;      // optional
    private double confidenceScore; // 0..1

    public String getFreshnessStatus() {
        if (purchaseDate == null || shelfLifeDays <= 0) return "UNKNOWN";
        long daysStored = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now());
        if (daysStored < shelfLifeDays * 0.6) return "FRESH";
        if (daysStored < shelfLifeDays) return "USE_SOON";
        return "EXPIRED";
    }
}