package com.rami.weeklymealplanner.mealplanner.application;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanRequest;
import com.rami.weeklymealplanner.mealplanner.infrastructure.OpenAiMealPlanHttpClient;

@Service
public class GenerateWeeklyPlanWithAiService {

    private static final String DEFAULT_PROMPT = """
            Generate a 7-day meal plan from the provided JSON payload.
            Requirements:
            - Respect people-specific calorie goals implied by sex/age/height/weight/targetLossLbsPerWeek.
            - Use only proteins listed in preferences.proteins. Other ingredients can vary.
            - Create diverse meals and specify how much each person can consume based on their calorie goals.
            - Respect allergies.excludedIngredients.
            - Respect days, mealsPerDay, maxPrepMinutesPerMeal, and maxCookMinutesPerMeal.
            - Deterministic, practical, and budget-aware meals.
            Return JSON only with this shape:
            {
              "plans": [
                {
                  "personId": "string",
                  "days": [
                    {
                      "dayNumber": 1,
                      "meals": [
                        {
                          "mealType": "BREAKFAST|LUNCH|DINNER",
                          "recipeId": "string",
                          "mealName": "string",
                          "primaryProtein": "string",
                          "estimatedCalories": 500,
                          "prepMinutes": 15,
                          "cookMinutes": 20,
                          "ingredients": [
                            {
                              "ingredient": "string",
                              "householdQuantity": 1.0,
                              "householdUnit": "cup",
                              "metricQuantity": 240.0,
                              "metricUnit": "g"
                            }
                          ],
                          "steps": ["string"]
                        }
                      ]
                    }
                  ]
                }
              ],
              "shoppingListRaw": [
                {
                  "ingredient": "string",
                  "totalHouseholdQuantity": 1.0,
                  "householdUnit": "cup",
                  "totalMetricQuantity": 240.0,
                  "metricUnit": "g"
                }
              ],
              "notes": ["string"]
            }
            """;

    private final OpenAiMealPlanHttpClient openAiMealPlanHttpClient;

    public GenerateWeeklyPlanWithAiService(OpenAiMealPlanHttpClient openAiMealPlanHttpClient) {
        this.openAiMealPlanHttpClient = openAiMealPlanHttpClient;
    }

    public JsonNode generate(GeneratePlanRequest request) throws IOException {
        validateRequest(request);
        return openAiMealPlanHttpClient.generate(request, DEFAULT_PROMPT);
    }

    private static void validateRequest(GeneratePlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.people() == null || request.people().isEmpty()) {
            throw new IllegalArgumentException("at least one person is required");
        }
        if (request.preferences() == null) {
            throw new IllegalArgumentException("preferences are required");
        }
    }
}
