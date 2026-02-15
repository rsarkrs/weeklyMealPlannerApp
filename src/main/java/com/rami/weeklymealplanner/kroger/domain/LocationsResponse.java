package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LocationsResponse(
        List<Location> data,
        Meta meta
) {}
