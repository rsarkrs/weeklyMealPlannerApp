package com.rami.weeklymealplanner.mealplanner.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FoodPreferencesRequest(
        @NotEmpty List<@NotBlank String> proteins,
        @NotEmpty List<@NotBlank String> veggies,
        @NotEmpty List<@NotBlank String> carbs
) {}
