package com.rami.weeklymealplanner.mealplanner.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.rami.weeklymealplanner.mealplanner.application.GenerateWeeklyPlanService;
import com.rami.weeklymealplanner.mealplanner.application.GenerateWeeklyPlanWithAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meal-planner")
public class MealPlannerController {

    private final GenerateWeeklyPlanService generateWeeklyPlanService;
    private final GenerateWeeklyPlanWithAiService generateWeeklyPlanWithAiService;

    public MealPlannerController(
            GenerateWeeklyPlanService generateWeeklyPlanService,
            GenerateWeeklyPlanWithAiService generateWeeklyPlanWithAiService
    ) {
        this.generateWeeklyPlanService = generateWeeklyPlanService;
        this.generateWeeklyPlanWithAiService = generateWeeklyPlanWithAiService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody GeneratePlanRequest request) {
        try {
            JsonNode response = generateWeeklyPlanWithAiService.generate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate weekly plan with OpenAI", "message", e.getMessage()));
        }
    }

    @PostMapping("/recipes/favorites")
    public ResponseEntity<?> markFavorite(@Valid @RequestBody FavoriteRecipeRequest request) {
        boolean saved = generateWeeklyPlanService.markFavorite(request.personId(), request.recipeId());
        if (!saved) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown recipeId. Generate a plan first and retry."));
        }
        return ResponseEntity.ok(Map.of("status", "saved", "personId", request.personId(), "recipeId", request.recipeId()));
    }

    @GetMapping("/recipes/favorites")
    public ResponseEntity<?> listFavorites(@RequestParam String personId) {
        if (personId == null || personId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "personId is required"));
        }
        return ResponseEntity.ok(new FavoriteRecipeResponse(personId, generateWeeklyPlanService.getFavorites(personId)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null
                ? firstError.getField() + " " + firstError.getDefaultMessage()
                : "invalid request";
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
