package com.rami.weeklymealplanner.mealplanner.api;

import com.rami.weeklymealplanner.mealplanner.domain.Sex;

public record PersonCalorieTargetResponse(
        String personId,
        Sex sex,
        Double ageYears,
        Double heightInches,
        Double weightLbs,
        Double requestedLossLbsPerWeek,
        Double bmr,
        Double tdee,
        Double dailyCalorieTarget,
        Double appliedCalorieFloor
) {}
