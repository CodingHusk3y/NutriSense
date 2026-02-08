package com.nutrisense.nutritionengine.controller;

import com.nutrisense.nutritionengine.model.*;
import com.nutrisense.nutritionengine.service.NutritionService;
import com.nutrisense.nutritionengine.service.ProfileService;
import com.nutrisense.nutritionengine.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/nutrition")
@CrossOrigin(origins = "*")
public class NutritionController {

    private final NutritionService nutritionService;
    private final RecommendationService recommendationService;
    private final ProfileService profileService;

    public NutritionController(NutritionService nutritionService,
                               RecommendationService recommendationService,
                               ProfileService profileService) {
        this.nutritionService = nutritionService;
        this.recommendationService = recommendationService;
        this.profileService = profileService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @PostMapping("/analyze")
    public NutritionResponse analyze(@RequestBody NutritionRequest request) {
        // 1) Resolve user profile
        UserProfile user = request.getUserProfile();

        if (user == null) {
            String userId = request.getUserId();
            if (userId != null && !userId.isBlank()) {
                try {
                    user = profileService.loadOrThrow(userId);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
                }
            }
        }

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Missing userProfile or userId"
            );
        }

        // 2) Ingredients
        List<Ingredient> ingredients = request.getIngredients();
        if (ingredients == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing ingredients");
        }

        // 3) Compute targets + recs
        NutritionTarget target = nutritionService.calculateTarget(user);
        FoodGroupTargets groupTargets = nutritionService.calculateFoodGroupTargets(user);

        List<FoodGap> gaps = recommendationService.detectGaps(user, ingredients);
        List<String> recs = recommendationService.generateRecommendations(user, ingredients, gaps);
        List<ShoppingItem> shopping = recommendationService.generateShoppingList(user, gaps);

        return new NutritionResponse(target, groupTargets, gaps, recs, shopping);
    }
}