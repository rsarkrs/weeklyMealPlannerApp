package com.rami.weeklymealplanner.kroger.api;

import java.util.List;

public record StoreMealEstimateResponse(
        String locationId,
        String locationName,
        int matchedCount,
        int missingCount,
        Double estimatedTotal,
        List<IngredientEstimateResponse> ingredients
) {}
