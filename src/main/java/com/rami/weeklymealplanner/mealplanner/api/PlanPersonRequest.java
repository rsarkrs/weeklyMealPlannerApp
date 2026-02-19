package com.rami.weeklymealplanner.mealplanner.api;

import com.rami.weeklymealplanner.mealplanner.domain.Sex;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanPersonRequest(
        @NotBlank String personId,
        @NotNull Sex sex,
        @NotNull @DecimalMin("1") Double ageYears,
        @NotNull @DecimalMin("1") Double heightInches,
        @NotNull @DecimalMin("1") Double weightLbs,
        @NotNull @DecimalMin("0") @DecimalMax("5") Double targetLossLbsPerWeek
) {}
