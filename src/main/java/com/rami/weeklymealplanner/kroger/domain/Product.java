package com.rami.weeklymealplanner.kroger.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(
        String productId,
        String upc,
        String description,
        String brand,
        ItemInformation itemInformation,
        Temperature temperature,
        List<ProductItem> items
) {}
