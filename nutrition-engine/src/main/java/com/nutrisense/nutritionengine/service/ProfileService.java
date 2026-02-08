package com.nutrisense.nutritionengine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutrisense.nutritionengine.model.UserProfile;
import com.nutrisense.nutritionengine.supabase.ProfileRow;
import com.nutrisense.nutritionengine.supabase.SupabaseRestClient;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final SupabaseRestClient supabase;

    public ProfileService(SupabaseRestClient supabase) {
        this.supabase = supabase;
    }

    public UserProfile loadOrThrow(String userId) {
        ProfileRow row = supabase.getProfileByUserId(userId);
        if (row == null) {
            throw new IllegalArgumentException("Profile not found for userId=" + userId);
        }

        UserProfile p = new UserProfile();

        // numbers
        p.setAge(row.age != null ? row.age : 20);
        p.setWeightKg(row.weight_kg != null ? row.weight_kg : 70.0);
        p.setHeightCm(row.height_cm != null ? row.height_cm.doubleValue() : 170.0);

        // strings (keep as-is, but normalize a bit)
        p.setGender(normalizeGender(row.gender));
        p.setHealthGoal(normalizeGoal(row.health_goal));
        p.setDietType(normalizeDiet(row.diet_type));

        // activity level not in schema -> read preferences jsonb if available
        p.setActivityLevel(readActivityLevel(row.preferences));

        return p;
    }

    private String normalizeGender(String g) {
        if (g == null) return "female"; // default (pick what you want)
        String x = g.trim().toLowerCase();
        if (x.equals("male") || x.equals("female")) return x;
        return x; // allow others if your FE sends
    }

    private String normalizeGoal(String goal) {
        if (goal == null) return "MAINTAIN";
        String x = goal.trim().toLowerCase();

        // DB -> Engine mapping
        switch (x) {
            case "lose":
            case "lose_weight":
            case "loss":
                return "LOSE_WEIGHT";
            case "gain":
            case "gain_muscle":
            case "muscle":
                return "GAIN_MUSCLE";
            case "maintain":
            case "maintain_weight":
                return "MAINTAIN";
            case "energy":
            case "increase_energy":
                return "INCREASE_ENERGY";
            default:
                // fallback: uppercase original (still better than raw)
                return goal.trim().toUpperCase();
        }
    }

    private String normalizeDiet(String diet) {
        if (diet == null) return "BALANCED";
        String x = diet.trim().toLowerCase();

        switch (x) {
            case "balanced": return "BALANCED";
            case "vegetarian": return "VEGETARIAN";
            case "vegan": return "VEGAN";
            case "keto": return "KETO";
            case "paleo": return "PALEO";
            case "mediterranean": return "MEDITERRANEAN";
            default:
                return diet.trim().toUpperCase();
        }
    }


    private String readActivityLevel(JsonNode preferences) {
        // default for demo
        String fallback = "Moderate";

        if (preferences == null || preferences.isNull()) return fallback;

        // preferences could be [] or {} depending on your setup
        // Try: { "activityLevel": "Light" }
        JsonNode n = preferences.get("activityLevel");
        if (n != null && n.isTextual()) return n.asText();

        // If preferences is an array of tags (like ["spicy","no dairy"]), ignore
        return fallback;
    }
}