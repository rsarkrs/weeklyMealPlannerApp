package com.rami.weeklymealplanner.kroger.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.kroger.application.EstimateWeeklyMealCostService;

@WebMvcTest(KrogerMealPlanController.class)
class KrogerMealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EstimateWeeklyMealCostService mealCostService;

    @Test
    void estimateReturnsBadRequestWhenZipIsBlank() throws Exception {
        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                " ",
                10,
                5,
                5,
                List.of(new IngredientInputRequest("Dinner", "rice", 1.0, "lb", null))
        );

        mockMvc.perform(post("/api/v1/kroger/meal-plan/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("zip is required"));
    }

    @Test
    void estimateReturnsBadRequestWhenIngredientsAreMissing() throws Exception {
        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                5,
                5,
                List.of()
        );

        mockMvc.perform(post("/api/v1/kroger/meal-plan/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("at least one ingredient is required"));
    }

    @Test
    void estimateReturnsBadRequestWhenIngredientQuantityInvalid() throws Exception {
        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                5,
                5,
                List.of(new IngredientInputRequest("Dinner", "rice", 0.0, "lb", null))
        );

        mockMvc.perform(post("/api/v1/kroger/meal-plan/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ingredient quantity must be > 0"));
    }

    @Test
    void estimateReturnsOkWhenServiceSucceeds() throws Exception {
        WeeklyMealEstimateResponse response = new WeeklyMealEstimateResponse(
                "85338",
                2,
                1,
                "01400433",
                "Store A",
                12.25,
                List.of(new StoreMealEstimateResponse(
                        "01400433",
                        "Store A",
                        2,
                        0,
                        12.25,
                        List.of(new IngredientEstimateResponse(
                                "Dinner",
                                "rice",
                                2.0,
                                "lb",
                                true,
                                "prod1",
                                "000111111111",
                                "Rice",
                                "Kroger",
                                "2 lb",
                                3.0,
                                6.0,
                                null
                        ))
                ))
        );

        when(mealCostService.estimate(any())).thenReturn(response);

        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                5,
                5,
                List.of(new IngredientInputRequest("Dinner", "rice", 2.0, "lb", null))
        );

        mockMvc.perform(post("/api/v1/kroger/meal-plan/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cheapestStoreLocationId").value("01400433"))
                .andExpect(jsonPath("$.stores[0].locationName").value("Store A"))
                .andExpect(jsonPath("$.stores[0].estimatedTotal").value(12.25));
    }

    @Test
    void estimateReturnsInternalServerErrorWhenServiceThrowsIOException() throws Exception {
        when(mealCostService.estimate(any()))
                .thenThrow(new IOException("Locations call failed: HTTP 401 Unauthorized"));

        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                5,
                5,
                List.of(new IngredientInputRequest("Dinner", "rice", 2.0, "lb", null))
        );

        mockMvc.perform(post("/api/v1/kroger/meal-plan/estimate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to estimate meal costs"))
                .andExpect(jsonPath("$.message", containsString("HTTP 401")));
    }
}
