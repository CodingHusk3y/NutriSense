package com.nutrisense.nutritionengine.supabase;

import com.fasterxml.jackson.databind.JsonNode;

public class ProfileRow {
    public String user_id;         // uuid as string
    public Integer age;
    public String gender;          // matches enum text from DB
    public Double weight_kg;
    public Integer height_cm;
    public String health_goal;
    public String diet_type;
    public JsonNode preferences;   // jsonb
}