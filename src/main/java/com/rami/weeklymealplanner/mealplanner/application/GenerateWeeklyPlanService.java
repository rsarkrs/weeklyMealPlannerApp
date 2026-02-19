package com.rami.weeklymealplanner.mealplanner.application;

import com.rami.weeklymealplanner.kroger.api.IngredientInputRequest;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateRequest;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateResponse;
import com.rami.weeklymealplanner.kroger.application.EstimateWeeklyMealCostService;
import com.rami.weeklymealplanner.mealplanner.api.FoodPreferencesRequest;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanRequest;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanResponse;
import com.rami.weeklymealplanner.mealplanner.api.IngredientQuantityResponse;
import com.rami.weeklymealplanner.mealplanner.api.MealDayResponse;
import com.rami.weeklymealplanner.mealplanner.api.MealResponse;
import com.rami.weeklymealplanner.mealplanner.api.PersonCalorieTargetResponse;
import com.rami.weeklymealplanner.mealplanner.api.PersonWeeklyPlanResponse;
import com.rami.weeklymealplanner.mealplanner.api.PlanPersonRequest;
import com.rami.weeklymealplanner.mealplanner.api.ShoppingIngredientResponse;
import com.rami.weeklymealplanner.mealplanner.domain.MealType;
import com.rami.weeklymealplanner.mealplanner.domain.NutritionCalculator;
import com.rami.weeklymealplanner.mealplanner.domain.OptimizationGoal;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GenerateWeeklyPlanService {

    private static final int DEFAULT_DAYS = 7;
    private static final int DEFAULT_MEALS_PER_DAY = 3;
    private static final int DEFAULT_RADIUS = 10;
    private static final int DEFAULT_STORE_LIMIT = 5;
    private static final int DEFAULT_PRODUCT_LIMIT = 5;
    private static final int DEFAULT_MAX_PREP_MINUTES = 20;
    private static final int DEFAULT_MAX_COOK_MINUTES = 30;
    private static final double[] CALORIE_SHARES = {0.30, 0.35, 0.35};

    private final NutritionCalculator nutritionCalculator;
    private final EstimateWeeklyMealCostService estimateWeeklyMealCostService;
    private final RecipePreferenceStore recipePreferenceStore;

    public GenerateWeeklyPlanService(
            NutritionCalculator nutritionCalculator,
            EstimateWeeklyMealCostService estimateWeeklyMealCostService,
            RecipePreferenceStore recipePreferenceStore
    ) {
        this.nutritionCalculator = nutritionCalculator;
        this.estimateWeeklyMealCostService = estimateWeeklyMealCostService;
        this.recipePreferenceStore = recipePreferenceStore;
    }

    public GeneratePlanResponse generate(GeneratePlanRequest request) throws IOException {
        validateCoreRequest(request);

        int days = positiveOrDefault(request.days(), DEFAULT_DAYS);
        int mealsPerDay = Math.min(
                positiveOrDefault(request.mealsPerDay(), DEFAULT_MEALS_PER_DAY),
                MealType.values().length
        );
        int radius = positiveOrDefault(request.radius(), DEFAULT_RADIUS);
        int storeLimit = positiveOrDefault(request.storeLimit(), DEFAULT_STORE_LIMIT);
        int productLimit = positiveOrDefault(request.productLimit(), DEFAULT_PRODUCT_LIMIT);
        int maxPrepMinutes = positiveOrDefault(request.maxPrepMinutesPerMeal(), DEFAULT_MAX_PREP_MINUTES);
        int maxCookMinutes = positiveOrDefault(request.maxCookMinutesPerMeal(), DEFAULT_MAX_COOK_MINUTES);
        OptimizationGoal optimizationGoal = request.optimizationGoal() != null
                ? request.optimizationGoal()
                : OptimizationGoal.BALANCED;

        Set<String> excludedIngredients = sanitizeSet(
                request.allergies() != null ? request.allergies().excludedIngredients() : List.of()
        );
        FoodPreferencesRequest preferences = request.preferences();
        List<String> proteins = sanitizeAndFilter(preferences.proteins(), excludedIngredients);
        List<String> veggies = sanitizeAndFilter(preferences.veggies(), excludedIngredients);
        List<String> carbs = sanitizeAndFilter(preferences.carbs(), excludedIngredients);
        if (proteins.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed protein is required after allergy filtering.");
        }
        if (veggies.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed veggie is required after allergy filtering.");
        }
        if (carbs.isEmpty()) {
            throw new IllegalArgumentException("At least one allowed carb is required after allergy filtering.");
        }

        List<PersonCalorieTargetResponse> calorieTargets = new ArrayList<>();
        List<PersonWeeklyPlanResponse> plans = new ArrayList<>();
        Map<String, AggregateIngredient> aggregateIngredients = new LinkedHashMap<>();

        for (PlanPersonRequest person : request.people()) {
            NutritionCalculator.NutritionResult nutrition = nutritionCalculator.calculate(
                    person.sex(),
                    person.ageYears(),
                    person.heightInches(),
                    person.weightLbs(),
                    person.targetLossLbsPerWeek()
            );

            calorieTargets.add(new PersonCalorieTargetResponse(
                    person.personId(),
                    person.sex(),
                    person.ageYears(),
                    person.heightInches(),
                    person.weightLbs(),
                    person.targetLossLbsPerWeek(),
                    nutrition.bmr(),
                    nutrition.tdee(),
                    nutrition.dailyCalorieTarget(),
                    nutrition.appliedFloor()
            ));

            List<MealDayResponse> personDays = buildPersonDays(
                    person.personId(),
                    days,
                    mealsPerDay,
                    nutrition.dailyCalorieTarget(),
                    proteins,
                    veggies,
                    carbs,
                    excludedIngredients,
                    optimizationGoal,
                    maxPrepMinutes,
                    maxCookMinutes
            );
            plans.add(new PersonWeeklyPlanResponse(person.personId(), personDays));

            List<MealResponse> meals = flattenMeals(personDays);
            recipePreferenceStore.saveGeneratedRecipes(person.personId(), meals);
            for (MealResponse meal : meals) {
                if (meal.ingredients() == null) {
                    continue;
                }
                for (IngredientQuantityResponse ingredient : meal.ingredients()) {
                    aggregateIngredients.computeIfAbsent(
                            normalizeIngredientKey(ingredient.ingredient(), ingredient.householdUnit(), ingredient.metricUnit()),
                            ignored -> new AggregateIngredient(
                                    ingredient.ingredient(),
                                    ingredient.householdUnit(),
                                    ingredient.metricUnit()
                            )
                    ).add(ingredient.householdQuantity(), ingredient.metricQuantity());
                }
            }
        }

        List<ShoppingIngredientResponse> shoppingListRaw = aggregateIngredients.values().stream()
                .map(item -> new ShoppingIngredientResponse(
                        item.name,
                        round2(item.totalHouseholdQuantity),
                        item.householdUnit,
                        round2(item.totalMetricQuantity),
                        item.metricUnit
                ))
                .sorted(Comparator.comparing(ShoppingIngredientResponse::ingredient, String.CASE_INSENSITIVE_ORDER))
                .toList();

        WeeklyMealEstimateResponse priced = estimateWeeklyMealCostService.estimate(new WeeklyMealEstimateRequest(
                request.zip().trim(),
                radius,
                storeLimit,
                productLimit,
                shoppingListRaw.stream()
                        .map(item -> new IngredientInputRequest(
                                "weekly-plan",
                                item.ingredient(),
                                item.totalHouseholdQuantity(),
                                item.householdUnit(),
                                null
                        ))
                        .toList()
        ));

        return new GeneratePlanResponse(
                request.zip().trim(),
                optimizationGoal,
                days,
                mealsPerDay,
                calorieTargets,
                plans,
                shoppingListRaw,
                priced
        );
    }

    public boolean markFavorite(String personId, String recipeId) {
        return recipePreferenceStore.markFavorite(personId, recipeId);
    }

    public List<MealResponse> getFavorites(String personId) {
        return recipePreferenceStore.getFavoriteRecipes(personId);
    }

    private List<MealDayResponse> buildPersonDays(
            String personId,
            int days,
            int mealsPerDay,
            double dailyTargetCalories,
            List<String> proteins,
            List<String> veggies,
            List<String> carbs,
            Set<String> excludedIngredients,
            OptimizationGoal optimizationGoal,
            int maxPrepMinutes,
            int maxCookMinutes
    ) {
        List<MealType> activeMealTypes = List.of(MealType.values()).subList(0, mealsPerDay);
        Map<MealType, ArrayDeque<MealResponse>> reusableFavorites = indexFavorites(personId, proteins, excludedIngredients);
        List<MealDayResponse> dayPlans = new ArrayList<>();

        for (int day = 1; day <= days; day++) {
            List<MealResponse> meals = new ArrayList<>();
            for (int mealIndex = 0; mealIndex < mealsPerDay; mealIndex++) {
                MealType mealType = activeMealTypes.get(mealIndex);
                int calories = mealCaloriesForIndex(mealIndex, mealsPerDay, dailyTargetCalories);

                MealResponse favorite = pollReusableFavorite(reusableFavorites, mealType);
                if (favorite != null) {
                    meals.add(cloneFavoriteForSchedule(personId, day, mealType, favorite, calories, maxPrepMinutes, maxCookMinutes));
                    continue;
                }

                String protein = selectItem(proteins, day, mealIndex, optimizationGoal, 0);
                String veggie = selectItem(veggies, day, mealIndex, optimizationGoal, 1);
                String carb = selectItem(carbs, day, mealIndex, optimizationGoal, 2);
                meals.add(buildGeneratedMeal(
                        personId,
                        day,
                        mealType,
                        protein,
                        veggie,
                        carb,
                        calories,
                        maxPrepMinutes,
                        maxCookMinutes,
                        excludedIngredients
                ));
            }
            dayPlans.add(new MealDayResponse(day, meals));
        }

        return dayPlans;
    }

    private static MealResponse buildGeneratedMeal(
            String personId,
            int day,
            MealType mealType,
            String protein,
            String veggie,
            String carb,
            int calories,
            int maxPrepMinutes,
            int maxCookMinutes,
            Set<String> excludedIngredients
    ) {
        String mealName = switch (mealType) {
            case BREAKFAST -> protein + " scramble with " + veggie + " and " + carb;
            case LUNCH -> "Herb " + protein + " bowl with " + veggie + " and " + carb;
            case DINNER -> protein + " skillet with " + veggie + " and " + carb;
        };

        int basePrep = switch (mealType) {
            case BREAKFAST -> 10;
            case LUNCH -> 15;
            case DINNER -> 15;
        };
        int baseCook = switch (mealType) {
            case BREAKFAST -> 12;
            case LUNCH -> 20;
            case DINNER -> 25;
        };
        int prep = Math.min(basePrep, maxPrepMinutes);
        int cook = Math.min(baseCook, maxCookMinutes);
        List<IngredientQuantityResponse> ingredients = buildMealIngredients(mealType, protein, veggie, carb, excludedIngredients);
        List<String> steps = List.of(
                "Prep " + protein + ", " + veggie + ", and " + carb + ".",
                "Season protein with salt, pepper, and garlic powder.",
                "Cook protein first, then add veggies and carbs until tender.",
                "Finish with olive oil or lemon to taste and serve."
        );

        return new MealResponse(
                mealType.name(),
                createScheduledRecipeId(personId, day, mealType),
                mealName,
                protein,
                calories,
                prep,
                cook,
                ingredients,
                steps
        );
    }

    private static MealResponse cloneFavoriteForSchedule(
            String personId,
            int day,
            MealType mealType,
            MealResponse favorite,
            int calories,
            int maxPrepMinutes,
            int maxCookMinutes
    ) {
        return new MealResponse(
                mealType.name(),
                createScheduledRecipeId(personId, day, mealType),
                favorite.mealName(),
                favorite.primaryProtein(),
                calories,
                Math.min(defaultIfNull(favorite.prepMinutes(), maxPrepMinutes), maxPrepMinutes),
                Math.min(defaultIfNull(favorite.cookMinutes(), maxCookMinutes), maxCookMinutes),
                favorite.ingredients(),
                favorite.steps()
        );
    }

    private static int defaultIfNull(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static List<IngredientQuantityResponse> buildMealIngredients(
            MealType mealType,
            String protein,
            String veggie,
            String carb,
            Set<String> excludedIngredients
    ) {
        List<IngredientQuantityResponse> ingredients = new ArrayList<>();

        if (mealType == MealType.BREAKFAST) {
            ingredients.add(new IngredientQuantityResponse(protein, 4.0, "oz", 113.4, "g"));
            ingredients.add(new IngredientQuantityResponse(veggie, 0.5, "cup", 75.0, "g"));
            ingredients.add(new IngredientQuantityResponse(carb, 0.75, "cup", 120.0, "g"));
        } else {
            ingredients.add(new IngredientQuantityResponse(protein, 6.0, "oz", 170.1, "g"));
            ingredients.add(new IngredientQuantityResponse(veggie, 1.0, "cup", 150.0, "g"));
            ingredients.add(new IngredientQuantityResponse(carb, 1.0, "cup", 160.0, "g"));
        }

        if (!excludedIngredients.contains("olive oil")) {
            ingredients.add(new IngredientQuantityResponse("olive oil", 1.0, "tbsp", 14.0, "g"));
        }
        return ingredients;
    }

    private Map<MealType, ArrayDeque<MealResponse>> indexFavorites(
            String personId,
            List<String> allowedProteins,
            Set<String> excludedIngredients
    ) {
        Set<String> proteinSet = allowedProteins.stream()
                .map(GenerateWeeklyPlanService::normalizeToken)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<MealType, ArrayDeque<MealResponse>> byType = new LinkedHashMap<>();
        for (MealType type : MealType.values()) {
            byType.put(type, new ArrayDeque<>());
        }

        for (MealResponse favorite : recipePreferenceStore.getFavoriteRecipes(personId)) {
            if (favorite == null || favorite.mealType() == null || favorite.primaryProtein() == null) {
                continue;
            }
            if (!proteinSet.contains(normalizeToken(favorite.primaryProtein()))) {
                continue;
            }
            if (containsExcludedIngredient(favorite, excludedIngredients)) {
                continue;
            }
            MealType type;
            try {
                type = MealType.valueOf(favorite.mealType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            byType.get(type).addLast(favorite);
        }

        return byType;
    }

    private static boolean containsExcludedIngredient(MealResponse recipe, Set<String> excludedIngredients) {
        if (recipe.ingredients() == null || recipe.ingredients().isEmpty()) {
            return false;
        }
        for (IngredientQuantityResponse ingredient : recipe.ingredients()) {
            if (ingredient == null || ingredient.ingredient() == null) {
                continue;
            }
            if (excludedIngredients.contains(normalizeToken(ingredient.ingredient()))) {
                return true;
            }
        }
        return false;
    }

    private static MealResponse pollReusableFavorite(Map<MealType, ArrayDeque<MealResponse>> reusableFavorites, MealType type) {
        ArrayDeque<MealResponse> queue = reusableFavorites.get(type);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        MealResponse recipe = queue.removeFirst();
        queue.addLast(recipe);
        return recipe;
    }

    private static String selectItem(List<String> values, int day, int mealIndex, OptimizationGoal goal, int offset) {
        if (values.size() == 1) {
            return values.getFirst();
        }
        int index;
        switch (goal) {
            case COST -> index = 0;
            case VARIETY -> index = Math.floorMod(day + mealIndex + offset, values.size());
            case BALANCED -> index = Math.floorMod((day * 2) + mealIndex + offset, values.size());
            default -> index = Math.floorMod(day + mealIndex + offset, values.size());
        }
        return values.get(index);
    }

    private static int mealCaloriesForIndex(int mealIndex, int mealsPerDay, double dailyTargetCalories) {
        double denominator = 0.0;
        for (int i = 0; i < mealsPerDay; i++) {
            denominator += CALORIE_SHARES[i];
        }
        double normalizedShare = CALORIE_SHARES[mealIndex] / denominator;
        return (int) Math.round(dailyTargetCalories * normalizedShare);
    }

    private static String createScheduledRecipeId(String personId, int day, MealType mealType) {
        return sanitizeId(personId) + "-d" + day + "-" + mealType.name().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeId(String token) {
        return token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static List<MealResponse> flattenMeals(List<MealDayResponse> personDays) {
        List<MealResponse> meals = new ArrayList<>();
        for (MealDayResponse day : personDays) {
            if (day == null || day.meals() == null) {
                continue;
            }
            meals.addAll(day.meals());
        }
        return meals;
    }

    private static String normalizeIngredientKey(String ingredient, String householdUnit, String metricUnit) {
        return normalizeToken(ingredient) + "|" + safeUnit(householdUnit) + "|" + safeUnit(metricUnit);
    }

    private static String safeUnit(String unit) {
        return unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> sanitizeAndFilter(List<String> values, Set<String> exclusions) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .filter(value -> !exclusions.contains(normalizeToken(value)))
                .toList();
    }

    private static Set<String> sanitizeSet(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(GenerateWeeklyPlanService::normalizeToken)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void validateCoreRequest(GeneratePlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.zip() == null || request.zip().isBlank()) {
            throw new IllegalArgumentException("zip is required");
        }
        if (request.people() == null || request.people().isEmpty()) {
            throw new IllegalArgumentException("at least one person is required");
        }
        if (request.preferences() == null) {
            throw new IllegalArgumentException("preferences are required");
        }
    }

    private static final class AggregateIngredient {
        private final String name;
        private final String householdUnit;
        private final String metricUnit;
        private double totalHouseholdQuantity;
        private double totalMetricQuantity;

        private AggregateIngredient(String name, String householdUnit, String metricUnit) {
            this.name = name;
            this.householdUnit = householdUnit;
            this.metricUnit = metricUnit;
        }

        private void add(Double householdQuantity, Double metricQuantity) {
            this.totalHouseholdQuantity += householdQuantity != null ? householdQuantity : 0.0;
            this.totalMetricQuantity += metricQuantity != null ? metricQuantity : 0.0;
        }
    }
}
