package com.nutrisense.nutritionengine.controller;

import com.nutrisense.nutritionengine.service.StoreRecommendationService;
import com.nutrisense.nutritionengine.store.StoreRecommendationResponse;
import com.nutrisense.nutritionengine.store.StoreRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "*")
public class StoreController {

    private final StoreRecommendationService storeService;

    public StoreController(StoreRecommendationService storeService) {
        this.storeService = storeService;
    }

    @PostMapping("/recommend")
    public StoreRecommendationResponse recommend(@RequestBody StoreRequest req) {
        return storeService.recommend(req.getLat(), req.getLng(), req.getNeededItems());
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }
}