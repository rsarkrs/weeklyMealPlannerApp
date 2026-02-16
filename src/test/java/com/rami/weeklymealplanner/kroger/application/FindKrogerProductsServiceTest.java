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

import com.rami.weeklymealplanner.kroger.api.ProductSummaryResponse;
import com.rami.weeklymealplanner.kroger.domain.ItemInformation;
import com.rami.weeklymealplanner.kroger.domain.Product;
import com.rami.weeklymealplanner.kroger.domain.ProductItem;
import com.rami.weeklymealplanner.kroger.domain.ProductPrice;
import com.rami.weeklymealplanner.kroger.domain.ProductsResponse;
import com.rami.weeklymealplanner.kroger.domain.Temperature;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerProductsHttpClient;

@ExtendWith(MockitoExtension.class)
class FindKrogerProductsServiceTest {

    @Mock
    private KrogerProductsHttpClient productsClient;

    @InjectMocks
    private FindKrogerProductsService service;

    @Test
    void findReturnsEmptyListWhenResponseIsNull() throws IOException {
        when(productsClient.getProducts("Kroger", "milk", "01400433", 5)).thenReturn(null);

        List<ProductSummaryResponse> out = service.find("milk", "01400433", "Kroger", 5);

        assertThat(out).isEmpty();
    }

    @Test
    void findReturnsEmptyListWhenDataIsMissing() throws IOException {
        when(productsClient.getProducts("Kroger", "milk", "01400433", 5))
                .thenReturn(new ProductsResponse(null, null));

        List<ProductSummaryResponse> out = service.find("milk", "01400433", "Kroger", 5);

        assertThat(out).isEmpty();
    }

    @Test
    void findMapsProductSummaryFields() throws IOException {
        Product product = new Product(
                "0001111041729",
                null,
                "Kroger 2% Milk",
                "Kroger",
                null,
                new Temperature("REFRIGERATED"),
                List.of(
                        new ProductItem(
                                "0001111041729",
                                "1 gal",
                                "UNIT",
                                new ProductPrice(3.99, 2.99, 3.99, 2.99),
                                new ProductPrice(4.29, 3.79, 4.29, 3.79),
                                new ItemInformation(new Temperature("REFRIGERATED"))
                        )
                )
        );

        when(productsClient.getProducts("Kroger", "milk", "01400433", 5))
                .thenReturn(new ProductsResponse(List.of(product), null));

        List<ProductSummaryResponse> out = service.find("milk", "01400433", "Kroger", 5);

        assertThat(out).hasSize(1);
        ProductSummaryResponse item = out.getFirst();
        assertThat(item.productId()).isEqualTo("0001111041729");
        assertThat(item.description()).isEqualTo("Kroger 2% Milk");
        assertThat(item.brand()).isEqualTo("Kroger");
        assertThat(item.upc()).isEqualTo("0001111041729");
        assertThat(item.size()).isEqualTo("1 gal");
        assertThat(item.regularPrice()).isEqualTo(3.99);
        assertThat(item.promoPrice()).isEqualTo(2.99);
        assertThat(item.regularPerUnitEstimate()).isEqualTo(3.99);
        assertThat(item.promoPerUnitEstimate()).isEqualTo(2.99);
        assertThat(item.nationalRegularPrice()).isEqualTo(4.29);
        assertThat(item.nationalPromoPrice()).isEqualTo(3.79);
        assertThat(item.nationalRegularPerUnitEstimate()).isEqualTo(4.29);
        assertThat(item.nationalPromoPerUnitEstimate()).isEqualTo(3.79);
        assertThat(item.soldBy()).isEqualTo("UNIT");
        assertThat(item.temperature()).isEqualTo("REFRIGERATED");
    }

    @Test
    void findFallsBackToProductUpcWhenItemUpcMissing() throws IOException {
        Product product = new Product(
                "123",
                "fallback-upc",
                "Whole Milk",
                "Simple Truth",
                null,
                null,
                List.of(
                        new ProductItem(
                                null,
                                "64 fl oz",
                                null,
                                new ProductPrice(4.49, null, 0.56, null),
                                null,
                                null
                        )
                )
        );

        when(productsClient.getProducts(null, "milk", "01400433", 5))
                .thenReturn(new ProductsResponse(List.of(product), null));

        List<ProductSummaryResponse> out = service.find("milk", "01400433", null, 5);

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().upc()).isEqualTo("fallback-upc");
        assertThat(out.getFirst().promoPrice()).isNull();
        assertThat(out.getFirst().regularPerUnitEstimate()).isEqualTo(0.56);
        assertThat(out.getFirst().nationalRegularPrice()).isNull();
    }
}
