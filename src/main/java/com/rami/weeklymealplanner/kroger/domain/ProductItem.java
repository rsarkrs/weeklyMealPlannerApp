package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductItem(
        String upc,
        String size,
        String soldBy,
        ProductPrice price,
        ItemInformation itemInformation
) {}
