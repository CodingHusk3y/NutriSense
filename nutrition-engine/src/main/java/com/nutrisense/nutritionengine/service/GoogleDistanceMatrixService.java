package com.nutrisense.nutritionengine.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleDistanceMatrixService {

    private final WebClient webClient;

    @Value("${google.maps.apiKey:}")
    private String apiKey;

    @Value("${google.maps.mode:driving}")
    private String mode; // driving, walking, bicycling, transit

    public GoogleDistanceMatrixService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * @return distance in km, or -1 if failed
     */
    public double drivingDistanceKm(double fromLat, double fromLng, double toLat, double toLng) {
        String safeMode = (mode == null ? "driving" : mode.trim());
        String safeKey = apiKey.trim();

        System.out.println("ENTER drivingDistanceKm, apiKeyPresent=" + (apiKey != null && !apiKey.isBlank()) + ", mode=" + mode);

        if (!isConfigured()) return -1;

        String origins = fromLat + "," + fromLng;
        String dests = toLat + "," + toLng;

        String url = "https://maps.googleapis.com/maps/api/distancematrix/json"
                + "?origins=" + origins
                + "&destinations=" + dests
                + "&mode=" + safeMode
                + "&key=" + safeKey;

        System.out.println("Google DM URL = " + url);


        try {
            GoogleDMResponse res = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("maps.googleapis.com")
                            .path("/maps/api/distancematrix/json")
                            .queryParam("origins", origins)
                            .queryParam("destinations", dests)
                            .queryParam("mode", safeMode)
                            .queryParam("key", safeKey)
                            .build())
                    .retrieve()
                    .bodyToMono(GoogleDMResponse.class)
                    .block();

            String raw = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("RAW GOOGLE RESPONSE: " + raw);

            System.out.println("Google response status=" + res.status);

            if (res == null || res.rows == null || res.rows.length == 0) return -1;
            if (res.rows[0].elements == null || res.rows[0].elements.length == 0) return -1;

            Element el = res.rows[0].elements[0];
            System.out.println("Google element status=" + el.status
                    + ", hasDistance=" + (el.distance != null)
                    + ", hasDuration=" + (el.duration != null));

            if (el == null || el.distance == null) return -1;
            if (el.status != null && !"OK".equalsIgnoreCase(el.status)) {
                System.out.println("Google element not OK: " + el.status);
                return -1;
            }
            double km = el.distance.value / 1000.0;
            double minutes = el.duration != null ? el.duration.value / 60.0 : -1;

            System.out.println("Google mode=" + mode +
                    " distanceKm=" + km +
                    " durationMin=" + minutes);


            // meters -> km
            return el.distance.value / 1000.0;

        } catch (Exception e) {
            System.out.println("Google DM exception: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ---- DTOs for JSON ----
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GoogleDMResponse {
        public Row[] rows;
        public String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Row {
        public Element[] elements;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Element {
        public Distance distance;
        public Duration duration;
        public String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Distance {
        public int value;   // meters
        public String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Duration {
        public int value;   // seconds
        public String text;
    }


}