package com.rami.weeklymealplanner.kroger.api;

public record ProductSummaryResponse(
        String productId,
        String description,
        String brand,
        String upc,
        String size,
        Double regularPrice,
        Double promoPrice,
        Double regularPerUnitEstimate,
        Double promoPerUnitEstimate,
        Double nationalRegularPrice,
        Double nationalPromoPrice,
        Double nationalRegularPerUnitEstimate,
        Double nationalPromoPerUnitEstimate,
        String soldBy,
        String temperature
) {}
