package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rami.weeklymealplanner.kroger.application.EstimateWeeklyMealCostService;

@RestController
@RequestMapping("/api/v1/kroger")
public class KrogerMealPlanController {

    private final EstimateWeeklyMealCostService mealCostService;

    public KrogerMealPlanController(EstimateWeeklyMealCostService mealCostService) {
        this.mealCostService = mealCostService;
    }

    @PostMapping("/meal-plan/estimate")
    public ResponseEntity<?> estimateMealPlan(@RequestBody(required = false) WeeklyMealEstimateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body is required"));
        }
        if (request.zip() == null || request.zip().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "zip is required"));
        }

        List<IngredientInputRequest> ingredients = request.ingredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "at least one ingredient is required"));
        }

        for (IngredientInputRequest ingredient : ingredients) {
            if (ingredient == null || ingredient.ingredient() == null || ingredient.ingredient().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ingredient name is required for each ingredient"));
            }
            if (ingredient.quantity() != null && ingredient.quantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "ingredient quantity must be > 0"));
            }
        }

        if (request.radius() != null && request.radius() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "radius must be > 0"));
        }
        if (request.storeLimit() != null && request.storeLimit() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "storeLimit must be > 0"));
        }
        if (request.productLimit() != null && request.productLimit() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "productLimit must be > 0"));
        }

        try {
            WeeklyMealEstimateResponse response = mealCostService.estimate(request);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to estimate meal costs", "message", e.getMessage()));
        }
    }
}
