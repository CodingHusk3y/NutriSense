package com.nutrisense.nutritionengine.supabase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreRow {
    private UUID id;
    private String name;
    private String chain;

    @JsonProperty("address")
    private String address;
    private Double lat;
    private Double lng;
}