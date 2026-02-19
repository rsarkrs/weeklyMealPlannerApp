package com.rami.weeklymealplanner.mealplanner.api;

import java.util.List;

public record MealDayResponse(
        Integer dayNumber,
        List<MealResponse> meals
) {}
