package com.rami.weeklymealplanner.kroger.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductsResponse(
        List<Product> data,
        Meta meta
) {}
