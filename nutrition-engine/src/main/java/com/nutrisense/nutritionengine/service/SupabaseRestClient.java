package com.nutrisense.nutritionengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SupabaseRestClient {

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anonKey}")
    private String anonKey;

    public SupabaseRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // GET https://<ref>.supabase.co/rest/v1/<pathAndQuery>
    public WebClient.ResponseSpec get(String pathAndQuery) {
        String url = supabaseUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);

        return webClient.get()
                .uri(url + "/rest/v1/" + pathAndQuery)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer " + anonKey)
                .retrieve();
    }
}