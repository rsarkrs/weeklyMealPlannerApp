package com.rami.weeklymealplanner.mealplanner.api;

import java.util.List;

public record FavoriteRecipeResponse(
        String personId,
        List<MealResponse> recipes
) {}
