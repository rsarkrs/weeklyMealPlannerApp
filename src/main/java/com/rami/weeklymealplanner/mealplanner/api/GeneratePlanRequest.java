package com.rami.weeklymealplanner.mealplanner.api;

import com.rami.weeklymealplanner.mealplanner.domain.OptimizationGoal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GeneratePlanRequest(
        String zip,
        @NotEmpty List<@Valid PlanPersonRequest> people,
        @NotNull @Valid FoodPreferencesRequest preferences,
        @Valid AllergiesRequest allergies,
        OptimizationGoal optimizationGoal,
        @Min(1) @Max(7) Integer days,
        @Min(1) @Max(3) Integer mealsPerDay,
        @Min(1) @Max(100) Integer radius,
        @Min(1) @Max(25) Integer storeLimit,
        @Min(1) @Max(50) Integer productLimit,
        @Min(1) @Max(180) Integer maxPrepMinutesPerMeal,
        @Min(1) @Max(240) Integer maxCookMinutesPerMeal
) {}
