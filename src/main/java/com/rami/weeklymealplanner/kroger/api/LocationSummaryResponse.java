package com.rami.weeklymealplanner.kroger.api;

public record LocationSummaryResponse(
        String locationId,
        String name,
        String chain,
        String city,
        String state,
        String zipCode,
        String phone,
        String timezone,
        String todayHours,
        boolean pickup,
        boolean delivery
) {}
