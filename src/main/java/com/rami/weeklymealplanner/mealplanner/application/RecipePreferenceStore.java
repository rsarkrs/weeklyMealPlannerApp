package com.rami.weeklymealplanner.mealplanner.application;

import com.rami.weeklymealplanner.mealplanner.api.MealResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RecipePreferenceStore {

    private final Map<String, MealResponse> recipesById = new ConcurrentHashMap<>();
    private final Map<String, LinkedHashSet<String>> favoritesByPerson = new ConcurrentHashMap<>();

    public void saveGeneratedRecipes(String personId, List<MealResponse> meals) {
        if (meals == null || meals.isEmpty()) {
            return;
        }
        for (MealResponse meal : meals) {
            if (meal == null || meal.recipeId() == null || meal.recipeId().isBlank()) {
                continue;
            }
            recipesById.put(meal.recipeId(), meal);
        }
        favoritesByPerson.computeIfAbsent(personId, key -> new LinkedHashSet<>());
    }

    public boolean markFavorite(String personId, String recipeId) {
        if (!recipesById.containsKey(recipeId)) {
            return false;
        }
        favoritesByPerson.computeIfAbsent(personId, key -> new LinkedHashSet<>()).add(recipeId);
        return true;
    }

    public List<MealResponse> getFavoriteRecipes(String personId) {
        Set<String> ids = favoritesByPerson.getOrDefault(personId, new LinkedHashSet<>());
        List<MealResponse> out = new ArrayList<>();
        for (String id : ids) {
            MealResponse recipe = recipesById.get(id);
            if (recipe != null) {
                out.add(recipe);
            }
        }
        return out;
    }
}
