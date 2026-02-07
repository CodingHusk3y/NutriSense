package com.nutrisense.nutritionengine.controller;

import com.nutrisense.nutritionengine.model.*;
import com.nutrisense.nutritionengine.service.NutritionService;
import com.nutrisense.nutritionengine.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
@CrossOrigin(origins = "*")
public class NutritionController {

    private final NutritionService nutritionService;
    private final RecommendationService recommendationService;

    public NutritionController(NutritionService nutritionService,
                               RecommendationService recommendationService) {
        this.nutritionService = nutritionService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @PostMapping("/analyze")
    public NutritionResponse analyze(@RequestBody NutritionRequest request) {
        UserProfile user = request.getUserProfile();
        List<Ingredient> ingredients = request.getIngredients();

        NutritionTarget target = nutritionService.calculateTarget(user);
        FoodGroupTargets groupTargets = nutritionService.calculateFoodGroupTargets(user);

        List<FoodGap> gaps = recommendationService.detectGaps(user, ingredients);
        List<String> recs = recommendationService.generateRecommendations(user, ingredients, gaps);
        List<ShoppingItem> shopping = recommendationService.generateShoppingList(user, gaps);

        return new NutritionResponse(target, groupTargets, gaps, recs, shopping);
    }
}