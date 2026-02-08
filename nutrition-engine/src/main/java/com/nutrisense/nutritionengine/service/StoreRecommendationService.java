package com.nutrisense.nutritionengine.service;

import com.nutrisense.nutritionengine.store.Store;
import com.nutrisense.nutritionengine.store.StoreRecommendationResponse;
import com.nutrisense.nutritionengine.store.StoreScore;
import com.nutrisense.nutritionengine.supabase.StoreRow;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StoreRecommendationService {

    private static final double WEIGHT_PRICE = 0.5;
    private static final double WEIGHT_DISTANCE = 0.5;
    private static final double MISSING_ITEM_PENALTY_PRICE = 6.00;

    private final GoogleDistanceMatrixService googleDistance;
    private final StoreCatalogService storeCatalogService;

    public StoreRecommendationService(GoogleDistanceMatrixService googleDistance,
                                      StoreCatalogService storeCatalogService) {
        this.googleDistance = googleDistance;
        this.storeCatalogService = storeCatalogService;
    }

    public StoreRecommendationResponse recommend(double userLat, double userLng, List<String> neededItems) {
        List<String> items = (neededItems == null) ? Collections.emptyList() : neededItems;

        // Load stores from DB
        List<StoreRow> storeRows = storeCatalogService.getStores();
        List<Store> stores = new ArrayList<>();
        for (StoreRow r : storeRows) {
            if (r.getId() == null || r.getName() == null|| r.getChain() == null || r.getLat() == null || r.getLng() == null) continue;

            stores.add(new Store(
                    r.getId().toString(),
                    r.getName(),
                    r.getChain(),
                    r.getAddress(),
                    r.getLat(),
                    r.getLng(),
                    Collections.emptyMap() // no more mock itemPrices
            ));
        }
        System.out.println("stores from db: " + storeRows.size());
        for (int i = 0; i < Math.min(3, storeRows.size()); i++) {
            StoreRow r = storeRows.get(i);
            System.out.println("id=" + r.getId() + " name=" + r.getName() + " address=" + r.getAddress());
        }


        List<Double> totals = new ArrayList<>();
        List<Double> dists = new ArrayList<>();
        Map<String, Double> storeTotalPrice = new HashMap<>();
        Map<String, Double> storeDistance = new HashMap<>();

        for (Store s : stores) {
            double total = computeTotalPriceFromDb(s.getId(), items);

            double dist = googleDistance.drivingDistanceKm(userLat, userLng, s.getLatitude(), s.getLongitude());
            if (dist < 0) dist = distanceKm(userLat, userLng, s.getLatitude(), s.getLongitude());

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
                    s.getChain(),
                    s.getAddress(),
                    round2(total),
                    round2(dist),
                    round3(normPrice),
                    round3(normDist),
                    round3(score)
            ));
        }

        scored.sort(Comparator.comparingDouble(StoreScore::getScore));

        StoreScore bestOverall = scored.isEmpty() ? null : scored.get(0);
        StoreScore cheapest = scored.stream().min(Comparator.comparingDouble(StoreScore::getTotalPrice)).orElse(bestOverall);
        StoreScore closest = scored.stream().min(Comparator.comparingDouble(StoreScore::getDistanceKm)).orElse(bestOverall);

        return new StoreRecommendationResponse(bestOverall, cheapest, closest, scored);
    }

    private double computeTotalPriceFromDb(String storeIdStr, List<String> items) {
        if (items == null || items.isEmpty()) return 0;

        UUID storeId;
        try {
            storeId = UUID.fromString(storeIdStr);
        } catch (Exception e) {
            // Return a high penalty so this invalid store is never recommended
            return 10000.0;
        }

        double total = 0;
        for (String item : items) {
            if (item == null) continue;
            String key = item.trim().toLowerCase();
            if (key.isBlank()) continue;

            double p = storeCatalogService.getPrice(storeId, key);
            total += (p >= 0) ? p : MISSING_ITEM_PENALTY_PRICE;
        }
        return total;
    }

    private double normalize(double x, double min, double max) {
        double denom = (max - min);
        if (denom == 0) return 0;
        return (x - min) / denom;
    }

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
}