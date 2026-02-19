package com.rami.weeklymealplanner.kroger.api;

import java.util.List;

public record WeeklyMealEstimateRequest(
        String zip,
        Integer radius,
        Integer storeLimit,
        Integer productLimit,
        List<IngredientInputRequest> ingredients
) {}
