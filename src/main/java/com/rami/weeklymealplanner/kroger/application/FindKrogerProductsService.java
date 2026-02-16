package com.rami.weeklymealplanner.kroger.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rami.weeklymealplanner.kroger.api.ProductSummaryResponse;
import com.rami.weeklymealplanner.kroger.domain.ItemInformation;
import com.rami.weeklymealplanner.kroger.domain.Product;
import com.rami.weeklymealplanner.kroger.domain.ProductItem;
import com.rami.weeklymealplanner.kroger.domain.ProductPrice;
import com.rami.weeklymealplanner.kroger.domain.ProductsResponse;
import com.rami.weeklymealplanner.kroger.domain.Temperature;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerProductsHttpClient;

@Service
public class FindKrogerProductsService {

    private final KrogerProductsHttpClient productsClient;

    public FindKrogerProductsService(KrogerProductsHttpClient productsClient) {
        this.productsClient = productsClient;
    }

    public List<ProductSummaryResponse> find(String term, String locationId, String brand, int limit) throws IOException {
        ProductsResponse response = productsClient.getProducts(brand, term, locationId, limit);

        List<ProductSummaryResponse> out = new ArrayList<>();
        if (response == null || response.data() == null) {
            return out;
        }

        for (Product product : response.data()) {
            if (product == null) {
                continue;
            }

            ProductItem item = firstItem(product.items());
            ProductPrice price = item != null ? item.price() : null;
            ProductPrice nationalPrice = item != null ? item.nationalPrice() : null;
            ItemInformation itemInfo = item != null ? item.itemInformation() : null;

            // Kroger commonly returns temperature under product.temperature.
            Temperature temperature = product.temperature();
            if (temperature == null) {
                temperature = itemInfo != null ? itemInfo.temperature() : null;
            }

            String productId = safe(product.productId());
            String description = safe(product.description());
            String productBrand = safe(product.brand());
            String upc = firstNonBlank(item != null ? safe(item.upc()) : "", safe(product.upc()));
            String size = item != null ? safe(item.size()) : "";
            Double regularPrice = price != null ? price.regular() : null;
            Double promoPrice = price != null ? price.promo() : null;
            Double regularPerUnitEstimate = price != null ? price.regularPerUnitEstimate() : null;
            Double promoPerUnitEstimate = price != null ? price.promoPerUnitEstimate() : null;
            Double nationalRegularPrice = nationalPrice != null ? nationalPrice.regular() : null;
            Double nationalPromoPrice = nationalPrice != null ? nationalPrice.promo() : null;
            Double nationalRegularPerUnitEstimate = nationalPrice != null ? nationalPrice.regularPerUnitEstimate() : null;
            Double nationalPromoPerUnitEstimate = nationalPrice != null ? nationalPrice.promoPerUnitEstimate() : null;
            String soldBy = item != null ? safe(item.soldBy()) : "";
            String tempIndicator = temperature != null ? safe(temperature.indicator()) : "";

            out.add(new ProductSummaryResponse(
                    productId,
                    description,
                    productBrand,
                    upc,
                    size,
                    regularPrice,
                    promoPrice,
                    regularPerUnitEstimate,
                    promoPerUnitEstimate,
                    nationalRegularPrice,
                    nationalPromoPrice,
                    nationalRegularPerUnitEstimate,
                    nationalPromoPerUnitEstimate,
                    soldBy,
                    tempIndicator
            ));
        }

        return out;
    }

    private static ProductItem firstItem(List<ProductItem> items) {
        if (items != null && !items.isEmpty()) {
            return items.get(0);
        }
        return null;
    }

    private static String firstNonBlank(String first, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return safe(fallback);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
