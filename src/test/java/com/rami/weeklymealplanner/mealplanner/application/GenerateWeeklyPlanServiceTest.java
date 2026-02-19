package com.rami.weeklymealplanner.mealplanner.application;

import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateResponse;
import com.rami.weeklymealplanner.kroger.application.EstimateWeeklyMealCostService;
import com.rami.weeklymealplanner.mealplanner.api.AllergiesRequest;
import com.rami.weeklymealplanner.mealplanner.api.FoodPreferencesRequest;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanRequest;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanResponse;
import com.rami.weeklymealplanner.mealplanner.api.PlanPersonRequest;
import com.rami.weeklymealplanner.mealplanner.domain.NutritionCalculator;
import com.rami.weeklymealplanner.mealplanner.domain.OptimizationGoal;
import com.rami.weeklymealplanner.mealplanner.domain.Sex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateWeeklyPlanServiceTest {

    @Mock
    private EstimateWeeklyMealCostService estimateWeeklyMealCostService;

    private GenerateWeeklyPlanService service;

    @BeforeEach
    void setUp() {
        service = new GenerateWeeklyPlanService(
                new NutritionCalculator(),
                estimateWeeklyMealCostService,
                new RecipePreferenceStore()
        );
    }

    @Test
    void generateCreatesPlanWithStrictProteinAdherenceAndPricingCall() throws IOException {
        when(estimateWeeklyMealCostService.estimate(any()))
                .thenReturn(new WeeklyMealEstimateResponse("85338", 4, 0, null, null, null, List.of()));

        GeneratePlanRequest request = new GeneratePlanRequest(
                "85338",
                List.of(new PlanPersonRequest("p1", Sex.FEMALE, 30.0, 65.0, 145.0, 1.0)),
                new FoodPreferencesRequest(
                        List.of("chicken breast", "salmon"),
                        List.of("broccoli", "spinach"),
                        List.of("rice", "potatoes")
                ),
                new AllergiesRequest(List.of("peanuts")),
                OptimizationGoal.VARIETY,
                7,
                3,
                10,
                5,
                5,
                20,
                30
        );

        GeneratePlanResponse response = service.generate(request);

        assertThat(response.plans()).hasSize(1);
        Set<String> allowedProteins = Set.of("chicken breast", "salmon");
        assertThat(response.plans().getFirst().days())
                .allSatisfy(day -> assertThat(day.meals())
                        .allSatisfy(meal -> assertThat(allowedProteins).contains(meal.primaryProtein())));

        ArgumentCaptor<com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateRequest> captor =
                ArgumentCaptor.forClass(com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateRequest.class);
        verify(estimateWeeklyMealCostService).estimate(captor.capture());
        assertThat(captor.getValue().ingredients()).isNotEmpty();
    }

    @Test
    void generateFailsWhenAllProteinsAreExcludedByAllergies() {
        GeneratePlanRequest request = new GeneratePlanRequest(
                "85338",
                List.of(new PlanPersonRequest("p1", Sex.MALE, 40.0, 70.0, 180.0, 1.0)),
                new FoodPreferencesRequest(
                        List.of("chicken"),
                        List.of("broccoli"),
                        List.of("rice")
                ),
                new AllergiesRequest(List.of("chicken")),
                OptimizationGoal.COST,
                7,
                3,
                10,
                5,
                5,
                20,
                30
        );

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one allowed protein");
    }
}
