package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Department(
        String departmentId,
        String name,
        String phone,
        Hours hours,
        Address address,
        Geolocation geolocation,
        Boolean offsite
) {}
