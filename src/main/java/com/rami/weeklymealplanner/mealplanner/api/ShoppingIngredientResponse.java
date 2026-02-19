package com.rami.weeklymealplanner.mealplanner.api;

public record ShoppingIngredientResponse(
        String ingredient,
        Double totalHouseholdQuantity,
        String householdUnit,
        Double totalMetricQuantity,
        String metricUnit
) {}
