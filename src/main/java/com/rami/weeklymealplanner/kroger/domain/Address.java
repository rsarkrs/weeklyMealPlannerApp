package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Address(
        String addressLine1,
        String city,
        String state,
        String zipCode,
        String county
) {}
