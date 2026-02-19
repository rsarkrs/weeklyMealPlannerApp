package com.rami.weeklymealplanner.mealplanner.api;

import jakarta.validation.constraints.NotBlank;

public record FavoriteRecipeRequest(
        @NotBlank String personId,
        @NotBlank String recipeId
) {}
