package com.rami.weeklymealplanner.kroger.api;

import java.util.List;

public record WeeklyMealEstimateResponse(
        String zip,
        int ingredientCount,
        int storeCount,
        String cheapestStoreLocationId,
        String cheapestStoreName,
        Double cheapestStoreTotal,
        List<StoreMealEstimateResponse> stores
) {}
