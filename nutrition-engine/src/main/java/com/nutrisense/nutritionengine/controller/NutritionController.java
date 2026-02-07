package com.nutrisense.nutritionengine.controller;

import com.nutrisense.nutritionengine.model.NutritionRequest;
import com.nutrisense.nutritionengine.model.NutritionResponse;
import com.nutrisense.nutritionengine.model.NutritionTarget;
import com.nutrisense.nutritionengine.model.ShoppingItem;
import com.nutrisense.nutritionengine.service.NutritionService;
import com.nutrisense.nutritionengine.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;
    private final RecommendationService recommendationService;

    public NutritionController(NutritionService nutritionService,
                               RecommendationService recommendationService) {
        this.nutritionService = nutritionService;
        this.recommendationService = recommendationService;
    }

    // Quick health check endpoint (for browser testing)
    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    // Main endpoint: compute targets + recommendations
    @PostMapping("/analyze")
    public NutritionResponse analyze(@RequestBody NutritionRequest request) {
        NutritionTarget target = nutritionService.calculateTarget(request.getUserProfile());

        List<String> recs = recommendationService.generateRecommendations(
                request.getUserProfile(),
                request.getIngredients()
        );

        List<ShoppingItem> shopping = recommendationService.generateShoppingList(
                request.getUserProfile(),
                request.getIngredients()
        );

        return new NutritionResponse(target, recs, shopping);
    }
}