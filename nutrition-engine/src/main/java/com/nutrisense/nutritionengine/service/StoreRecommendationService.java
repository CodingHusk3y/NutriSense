package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.store.Store;
import com.nutrisense.nutritionengine.store.StoreRecommendationResponse;
import com.nutrisense.nutritionengine.store.StoreScore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StoreRecommendationService {

    // weights from your README
    private static final double WEIGHT_PRICE = 0.5;
    private static final double WEIGHT_DISTANCE = 0.5;

    // if a store doesn't have a price for an item, we penalize it with fallback price
    private static final double MISSING_ITEM_PENALTY_PRICE = 6.00;

    public StoreRecommendationResponse recommend(double userLat, double userLng, List<String> neededItems) {
        List<String> items = (neededItems == null) ? Collections.emptyList() : neededItems;

        List<Store> stores = getMockStores();

        // collect raw totals
        List<Double> totals = new ArrayList<>();
        List<Double> dists = new ArrayList<>();
        Map<String, Double> storeTotalPrice = new HashMap<>();
        Map<String, Double> storeDistance = new HashMap<>();

        for (Store s : stores) {
            double total = computeTotalPrice(s, items);
            double dist = distanceKm(userLat, userLng, s.getLatitude(), s.getLongitude());

            storeTotalPrice.put(s.getId(), total);
            storeDistance.put(s.getId(), dist);

            totals.add(total);
            dists.add(dist);
        }

        double minPrice = totals.isEmpty() ? 0 : Collections.min(totals);
        double maxPrice = totals.isEmpty() ? 0 : Collections.max(totals);
        double minDist = dists.isEmpty() ? 0 : Collections.min(dists);
        double maxDist = dists.isEmpty() ? 0 : Collections.max(dists);

        List<StoreScore> scored = new ArrayList<>();
        for (Store s : stores) {
            double total = storeTotalPrice.get(s.getId());
            double dist = storeDistance.get(s.getId());

            double normPrice = normalize(total, minPrice, maxPrice);
            double normDist = normalize(dist, minDist, maxDist);

            double score = WEIGHT_PRICE * normPrice + WEIGHT_DISTANCE * normDist;

            scored.add(new StoreScore(
                    s.getId(),
                    s.getName(),
                    round2(total),
                    round2(dist),
                    round3(normPrice),
                    round3(normDist),
                    round3(score)
            ));
        }

        // sort by score asc (lower better)
        scored.sort(Comparator.comparingDouble(StoreScore::getScore));

        StoreScore bestOverall = scored.isEmpty() ? null : scored.get(0);
        StoreScore cheapest = scored.stream()
                .min(Comparator.comparingDouble(StoreScore::getTotalPrice))
                .orElse(bestOverall);
        StoreScore closest = scored.stream()
                .min(Comparator.comparingDouble(StoreScore::getDistanceKm))
                .orElse(bestOverall);

        return new StoreRecommendationResponse(bestOverall, cheapest, closest, scored);
    }

    private double computeTotalPrice(Store store, List<String> items) {
        if (items == null || items.isEmpty()) return 0;

        double total = 0;
        Map<String, Double> priceMap = store.getItemPrices() == null ? Collections.emptyMap() : store.getItemPrices();

        for (String item : items) {
            if (item == null) continue;
            Double p = priceMap.get(item);
            if (p == null) {
                // try case-insensitive lookup
                p = lookupIgnoreCase(priceMap, item);
            }
            total += (p != null) ? p : MISSING_ITEM_PENALTY_PRICE;
        }
        return total;
    }

    private Double lookupIgnoreCase(Map<String, Double> map, String key) {
        String k = key.toLowerCase();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase().equals(k)) return e.getValue();
        }
        return null;
    }

    // normalization to [0,1]
    private double normalize(double x, double min, double max) {
        double denom = (max - min);
        if (denom == 0) return 0; // all equal => everyone same normalized value
        return (x - min) / denom;
    }

    // Haversine distance in km
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }

    private double round2(double x) { return Math.round(x * 100.0) / 100.0; }
    private double round3(double x) { return Math.round(x * 1000.0) / 1000.0; }

    // ---- MOCK DATA for demo ----
    private List<Store> getMockStores() {
        List<Store> stores = new ArrayList<>();

        stores.add(new Store(
                "walmart",
                "Walmart",
                33.7756, -84.3963,
                Map.of(
                        "Chicken breast", 5.99,
                        "Greek yogurt", 1.59,
                        "Broccoli", 1.49,
                        "Spinach", 2.29,
                        "Apples", 3.99,
                        "Berries", 4.49,
                        "Oats", 3.19,
                        "Beans/Lentils", 2.99,
                        "Brown rice", 2.49,
                        "Whole-grain bread", 3.49
                )
        ));

        stores.add(new Store(
                "target",
                "Target",
                33.7790, -84.3880,
                Map.of(
                        "Chicken breast", 6.49,
                        "Greek yogurt", 1.79,
                        "Broccoli", 1.79,
                        "Spinach", 2.49,
                        "Apples", 4.29,
                        "Berries", 4.99,
                        "Oats", 3.49,
                        "Beans/Lentils", 3.29,
                        "Brown rice", 2.79,
                        "Whole-grain bread", 3.69
                )
        ));

        stores.add(new Store(
                "traderjoes",
                "Trader Joe's",
                33.7701, -84.3850,
                Map.of(
                        "Chicken breast", 6.99,
                        "Greek yogurt", 1.39,
                        "Broccoli", 1.29,
                        "Spinach", 1.99,
                        "Apples", 3.49,
                        "Berries", 4.29,
                        "Oats", 2.99,
                        "Beans/Lentils", 2.69,
                        "Brown rice", 2.59,
                        "Whole-grain bread", 3.29
                )
        ));

        return stores;
    }
}