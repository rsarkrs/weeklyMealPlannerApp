package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DayHours(
        String open,
        String close,
        Boolean open24
) {}
