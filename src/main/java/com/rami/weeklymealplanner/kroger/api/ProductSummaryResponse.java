package com.rami.weeklymealplanner.kroger.api;

public record ProductSummaryResponse(
        String productId,
        String description,
        String brand,
        String upc,
        String size,
        Double regularPrice,
        Double promoPrice,
        String soldBy,
        String temperature
) {}
