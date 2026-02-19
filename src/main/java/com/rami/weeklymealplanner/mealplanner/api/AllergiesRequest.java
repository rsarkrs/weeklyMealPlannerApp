package com.rami.weeklymealplanner.mealplanner.api;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AllergiesRequest(
        List<@NotBlank String> excludedIngredients
) {}
