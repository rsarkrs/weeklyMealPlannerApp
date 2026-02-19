package com.rami.weeklymealplanner.kroger.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rami.weeklymealplanner.kroger.api.IngredientEstimateResponse;
import com.rami.weeklymealplanner.kroger.api.IngredientInputRequest;
import com.rami.weeklymealplanner.kroger.api.LocationSummaryResponse;
import com.rami.weeklymealplanner.kroger.api.ProductSummaryResponse;
import com.rami.weeklymealplanner.kroger.api.StoreMealEstimateResponse;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateRequest;
import com.rami.weeklymealplanner.kroger.api.WeeklyMealEstimateResponse;

@Service
public class EstimateWeeklyMealCostService {

    private final FindKrogerLocationsService locationsService;
    private final FindKrogerProductsService productsService;

    public EstimateWeeklyMealCostService(
            FindKrogerLocationsService locationsService,
            FindKrogerProductsService productsService
    ) {
        this.locationsService = locationsService;
        this.productsService = productsService;
    }

    public WeeklyMealEstimateResponse estimate(WeeklyMealEstimateRequest request) throws IOException {
        String zip = safe(request.zip());
        int radius = positiveOrDefault(request.radius(), 10);
        int storeLimit = positiveOrDefault(request.storeLimit(), 5);
        int productLimit = positiveOrDefault(request.productLimit(), 5);
        List<IngredientInputRequest> ingredients = nonNullIngredients(request.ingredients());

        List<LocationSummaryResponse> locations = locationsService.find(zip, radius, storeLimit);
        List<StoreMealEstimateResponse> stores = new ArrayList<>();

        for (LocationSummaryResponse location : locations) {
            String locationId = safe(location.locationId());
            String locationName = safe(location.name());
            List<IngredientEstimateResponse> ingredientEstimates = new ArrayList<>();
            int matchedCount = 0;
            int missingCount = 0;
            double total = 0.0;

            for (IngredientInputRequest ingredient : ingredients) {
                String term = safe(ingredient.ingredient());
                String brand = blankToNull(ingredient.brand());
                double quantity = quantityOrDefault(ingredient.quantity(), 1.0);

                List<ProductSummaryResponse> products = productsService.find(term, locationId, brand, productLimit);
                ProductSummaryResponse best = selectBestPricedProduct(products);
                Double unitPrice = best != null ? comparableUnitPrice(best) : null;

                if (best == null || unitPrice == null) {
                    missingCount++;
                    ingredientEstimates.add(new IngredientEstimateResponse(
                            blankToNull(ingredient.meal()),
                            term,
                            quantity,
                            blankToNull(ingredient.unit()),
                            false,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "No priced product found for ingredient at this store"
                    ));
                    continue;
                }

                matchedCount++;
                double estimatedCost = unitPrice * quantity;
                total += estimatedCost;

                ingredientEstimates.add(new IngredientEstimateResponse(
                        blankToNull(ingredient.meal()),
                        term,
                        quantity,
                        blankToNull(ingredient.unit()),
                        true,
                        safe(best.productId()),
                        safe(best.upc()),
                        safe(best.description()),
                        safe(best.brand()),
                        safe(best.size()),
                        unitPrice,
                        estimatedCost,
                        null
                ));
            }

            stores.add(new StoreMealEstimateResponse(
                    locationId,
                    locationName,
                    matchedCount,
                    missingCount,
                    total,
                    ingredientEstimates
            ));
        }

        stores.sort(
                Comparator.comparingInt(StoreMealEstimateResponse::missingCount)
                        .thenComparingDouble(item -> item.estimatedTotal() != null ? item.estimatedTotal() : Double.MAX_VALUE)
        );

        StoreMealEstimateResponse cheapest = stores.isEmpty() ? null : stores.getFirst();

        return new WeeklyMealEstimateResponse(
                zip,
                ingredients.size(),
                stores.size(),
                cheapest != null ? cheapest.locationId() : null,
                cheapest != null ? cheapest.locationName() : null,
                cheapest != null ? cheapest.estimatedTotal() : null,
                stores
        );
    }

    private static ProductSummaryResponse selectBestPricedProduct(List<ProductSummaryResponse> products) {
        if (products == null || products.isEmpty()) {
            return null;
        }

        ProductSummaryResponse best = null;
        Double bestPrice = null;
        for (ProductSummaryResponse candidate : products) {
            if (candidate == null) {
                continue;
            }
            Double candidatePrice = comparableUnitPrice(candidate);
            if (candidatePrice == null) {
                continue;
            }
            if (bestPrice == null || candidatePrice < bestPrice) {
                best = candidate;
                bestPrice = candidatePrice;
            }
        }
        return best;
    }

    private static Double comparableUnitPrice(ProductSummaryResponse product) {
        return firstPositive(
                product.promoPerUnitEstimate(),
                product.regularPerUnitEstimate(),
                product.promoPrice(),
                product.regularPrice(),
                product.nationalPromoPerUnitEstimate(),
                product.nationalRegularPerUnitEstimate(),
                product.nationalPromoPrice(),
                product.nationalRegularPrice()
        );
    }

    private static Double firstPositive(Double... values) {
        for (Double value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private static List<IngredientInputRequest> nonNullIngredients(List<IngredientInputRequest> ingredients) {
        if (ingredients == null) {
            return List.of();
        }

        List<IngredientInputRequest> normalized = new ArrayList<>();
        for (IngredientInputRequest ingredient : ingredients) {
            if (ingredient == null) {
                continue;
            }
            if (safe(ingredient.ingredient()).isBlank()) {
                continue;
            }
            normalized.add(ingredient);
        }
        return normalized;
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static double quantityOrDefault(Double value, double defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        String safe = safe(value);
        return safe.isBlank() ? null : safe;
    }
}
