package com.rami.weeklymealplanner.mealplanner.api;

import java.util.List;

public record MealResponse(
        String mealType,
        String recipeId,
        String mealName,
        String primaryProtein,
        Integer estimatedCalories,
        Integer prepMinutes,
        Integer cookMinutes,
        List<IngredientQuantityResponse> ingredients,
        List<String> steps
) {}
