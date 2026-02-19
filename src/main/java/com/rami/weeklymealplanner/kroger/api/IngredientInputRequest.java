package com.rami.weeklymealplanner.kroger.api;

public record IngredientInputRequest(
        String meal,
        String ingredient,
        Double quantity,
        String unit,
        String brand
) {}
