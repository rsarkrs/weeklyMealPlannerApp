package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Location(
        String locationId,
        String storeNumber,
        String divisionNumber,
        String chain,
        String name,
        String phone,
        Address address,
        Geolocation geolocation,
        Hours hours,
        List<Department> departments
) {}
