package com.rami.weeklymealplanner.kroger.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Hours(
        String timezone,
        String gmtOffset,
        Boolean open24,
        DayHours monday,
        DayHours tuesday,
        DayHours wednesday,
        DayHours thursday,
        DayHours friday,
        DayHours saturday,
        DayHours sunday
) {}
