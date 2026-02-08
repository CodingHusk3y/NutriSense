package com.nutrisense.nutritionengine.supabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class SupabaseRestClient {

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.serviceRoleKey}")
    private String serviceRoleKey;

    public SupabaseRestClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // NEW: generic list fetch from /rest/v1
    public <T> List<T> getList(String pathAndQuery, Class<T> clazz) {
        String url = supabaseUrl + "/rest/v1/" + stripLeadingSlash(pathAndQuery);
        System.out.println("Supabase GET URL = " + url);
        System.out.println("Using serviceRoleKey? " + (serviceRoleKey != null && !serviceRoleKey.isBlank()));

        return webClient.get()
                .uri(url)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .retrieve()
                .bodyToFlux(clazz)
                .collectList()
                .block();
    }

    private String stripLeadingSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s.substring(1) : s;
    }

    public ProfileRow getProfileByUserId(String userId) {
        String url = supabaseUrl + "/rest/v1/profiles"
                + "?user_id=eq." + userId
                + "&select=user_id,age,gender,weight_kg,height_cm,health_goal,diet_type,preferences";

        System.out.println("Supabase profile URL = " + url);
        System.out.println("Using serviceRoleKey? " + (serviceRoleKey != null && !serviceRoleKey.isBlank()));

        return webClient.get()
                .uri(url)
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .exchangeToMono(resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.out.println("Supabase status=" + resp.statusCode());
                                    System.out.println("Supabase raw body=" + body);

                                    if (body == null || body.trim().equals("[]")) {
                                        return reactor.core.publisher.Mono.empty();
                                    }

                                    try {
                                        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                        com.fasterxml.jackson.databind.JsonNode arr = om.readTree(body);
                                        if (!arr.isArray() || arr.size() == 0) return reactor.core.publisher.Mono.empty();
                                        ProfileRow row = om.treeToValue(arr.get(0), ProfileRow.class);
                                        return reactor.core.publisher.Mono.just(row);
                                    } catch (Exception e) {
                                        return reactor.core.publisher.Mono.empty();
                                    }
                                })
                )
                .blockOptional()
                .orElse(null);
    }

}