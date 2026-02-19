package com.rami.weeklymealplanner.mealplanner.api;

public record IngredientQuantityResponse(
        String ingredient,
        Double householdQuantity,
        String householdUnit,
        Double metricQuantity,
        String metricUnit
) {}
