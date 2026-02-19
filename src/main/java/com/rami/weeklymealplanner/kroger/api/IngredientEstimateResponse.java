package com.rami.weeklymealplanner.kroger.api;

public record IngredientEstimateResponse(
        String meal,
        String ingredient,
        Double quantity,
        String unit,
        boolean matched,
        String productId,
        String upc,
        String description,
        String brand,
        String size,
        Double unitPrice,
        Double estimatedCost,
        String message
) {}
