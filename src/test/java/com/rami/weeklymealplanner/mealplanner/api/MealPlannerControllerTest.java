package com.rami.weeklymealplanner.mealplanner.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.mealplanner.application.GenerateWeeklyPlanService;
import com.rami.weeklymealplanner.mealplanner.application.GenerateWeeklyPlanWithAiService;
import com.rami.weeklymealplanner.mealplanner.domain.OptimizationGoal;
import com.rami.weeklymealplanner.mealplanner.domain.Sex;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealPlannerController.class)
class MealPlannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GenerateWeeklyPlanService generateWeeklyPlanService;

    @MockBean
    private GenerateWeeklyPlanWithAiService generateWeeklyPlanWithAiService;

    @Test
    void generateReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/meal-planner/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void favoritesReturnsBadRequestWhenPersonIdMissing() throws Exception {
        mockMvc.perform(get("/api/v1/meal-planner/recipes/favorites")
                        .queryParam("personId", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("personId is required"));
    }

    @Test
    void markFavoriteReturnsBadRequestWhenRecipeUnknown() throws Exception {
        when(generateWeeklyPlanService.markFavorite("p1", "r-1")).thenReturn(false);

        mockMvc.perform(post("/api/v1/meal-planner/recipes/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("personId", "p1", "recipeId", "r-1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown recipeId. Generate a plan first and retry."));
    }

    @Test
    void markFavoriteReturnsOkWhenSaved() throws Exception {
        when(generateWeeklyPlanService.markFavorite("p1", "r-1")).thenReturn(true);

        mockMvc.perform(post("/api/v1/meal-planner/recipes/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("personId", "p1", "recipeId", "r-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"));
    }

    @Test
    void generateReturnsOkWhenServiceSucceeds() throws Exception {
        GeneratePlanRequest request = new GeneratePlanRequest(
                null,
                List.of(new PlanPersonRequest("p1", Sex.FEMALE, 30.0, 65.0, 145.0, 1.0)),
                new FoodPreferencesRequest(List.of("chicken"), List.of("broccoli"), List.of("rice")),
                new AllergiesRequest(List.of()),
                OptimizationGoal.BALANCED,
                7,
                3,
                10,
                5,
                5,
                20,
                30
        );

        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("provider", "openai");
        response.set("generatedPlan", JsonNodeFactory.instance.objectNode().putArray("plans"));
        when(generateWeeklyPlanWithAiService.generate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/meal-planner/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("openai"));
    }
}
