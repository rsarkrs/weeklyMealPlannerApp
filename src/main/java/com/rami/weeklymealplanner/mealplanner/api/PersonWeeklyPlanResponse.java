package com.rami.weeklymealplanner.mealplanner.api;

import java.util.List;

public record PersonWeeklyPlanResponse(
        String personId,
        List<MealDayResponse> days
) {}
