package com.rami.weeklymealplanner.mealplanner.api;

import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateResponse;
import com.rami.weeklymealplanner.mealplanner.domain.OptimizationGoal;

import java.util.List;

public record GeneratePlanResponse(
        String zip,
        OptimizationGoal optimizationGoal,
        Integer days,
        Integer mealsPerDay,
        List<PersonCalorieTargetResponse> calorieTargets,
        List<PersonWeeklyPlanResponse> plans,
        List<ShoppingIngredientResponse> shoppingListRaw,
        WeeklyMealEstimateResponse shoppingListPriced
) {}
