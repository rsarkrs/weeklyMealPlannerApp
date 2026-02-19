package com.rami.weeklymealplanner.kroger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rami.weeklymealplanner.kroger.api.IngredientInputRequest;
import com.rami.weeklymealplanner.kroger.api.LocationSummaryResponse;
import com.rami.weeklymealplanner.kroger.api.ProductSummaryResponse;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateRequest;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateResponse;

@ExtendWith(MockitoExtension.class)
class EstimateWeeklyMealCostServiceTest {

    @Mock
    private FindKrogerLocationsService locationsService;

    @Mock
    private FindKrogerProductsService productsService;

    @InjectMocks
    private EstimateWeeklyMealCostService service;

    @Test
    void estimateReturnsCheapestStoreByComputedIngredientTotals() throws IOException {
        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                2,
                5,
                List.of(
                        new IngredientInputRequest("Breakfast", "eggs", 1.0, "dozen", null),
                        new IngredientInputRequest("Dinner", "rice", 2.0, "lb", null)
                )
        );

        when(locationsService.find("85338", 10, 2)).thenReturn(List.of(
                location("01400433", "Store A"),
                location("01400434", "Store B")
        ));

        when(productsService.find("eggs", "01400433", null, 5)).thenReturn(List.of(product("Eggs A", 2.50)));
        when(productsService.find("rice", "01400433", null, 5)).thenReturn(List.of(product("Rice A", 1.75)));

        when(productsService.find("eggs", "01400434", null, 5)).thenReturn(List.of(product("Eggs B", 2.20)));
        when(productsService.find("rice", "01400434", null, 5)).thenReturn(List.of(product("Rice B", 2.10)));

        WeeklyMealEstimateResponse response = service.estimate(request);

        assertThat(response.storeCount()).isEqualTo(2);
        assertThat(response.ingredientCount()).isEqualTo(2);
        assertThat(response.cheapestStoreLocationId()).isEqualTo("01400433");
        assertThat(response.cheapestStoreTotal()).isEqualTo(6.0);

        assertThat(response.stores()).hasSize(2);
        assertThat(response.stores().getFirst().locationId()).isEqualTo("01400433");
        assertThat(response.stores().getFirst().missingCount()).isEqualTo(0);
        assertThat(response.stores().getFirst().estimatedTotal()).isEqualTo(6.0);
    }

    @Test
    void estimateTracksMissingIngredientsPerStore() throws IOException {
        WeeklyMealEstimateRequest request = new WeeklyMealEstimateRequest(
                "85338",
                10,
                1,
                5,
                List.of(
                        new IngredientInputRequest("Breakfast", "eggs", 1.0, "dozen", null),
                        new IngredientInputRequest("Dinner", "milk", 1.0, "gal", null)
                )
        );

        when(locationsService.find("85338", 10, 1)).thenReturn(List.of(location("01400433", "Store A")));
        when(productsService.find("eggs", "01400433", null, 5)).thenReturn(List.of(product("Eggs A", 2.5)));
        when(productsService.find("milk", "01400433", null, 5)).thenReturn(List.of());

        WeeklyMealEstimateResponse response = service.estimate(request);

        assertThat(response.storeCount()).isEqualTo(1);
        assertThat(response.cheapestStoreLocationId()).isEqualTo("01400433");
        assertThat(response.stores().getFirst().matchedCount()).isEqualTo(1);
        assertThat(response.stores().getFirst().missingCount()).isEqualTo(1);
        assertThat(response.stores().getFirst().estimatedTotal()).isEqualTo(2.5);
        assertThat(response.stores().getFirst().ingredients()).hasSize(2);
        assertThat(response.stores().getFirst().ingredients().get(1).matched()).isFalse();
    }

    private static LocationSummaryResponse location(String id, String name) {
        return new LocationSummaryResponse(
                id,
                name,
                "Kroger",
                "Goodyear",
                "AZ",
                "85338",
                "",
                "",
                "",
                true,
                true
        );
    }

    private static ProductSummaryResponse product(String description, Double promoPerUnit) {
        return new ProductSummaryResponse(
                "prod-1",
                description,
                "Kroger",
                "0000000000000",
                "1 unit",
                promoPerUnit,
                promoPerUnit,
                promoPerUnit,
                promoPerUnit,
                null,
                null,
                null,
                null,
                "UNIT",
                "AMBIENT"
        );
    }
}
