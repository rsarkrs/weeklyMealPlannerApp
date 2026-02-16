package com.rami.weeklymealplanner.kroger.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.domain.ProductsResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(MockitoExtension.class)
class KrogerProductsHttpClientTest {

    private MockWebServer mockWebServer;

    @Mock
    private KrogerAuthHttpClient authClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getProductsSendsExpectedQueryAndAuthorizationAndParsesBody() throws Exception {
        when(authClient.getAccessToken()).thenReturn("access-token");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "data": [
                            {
                              "productId": "0001111041729",
                              "description": "Kroger 2% Milk",
                              "temperature": { "indicator": "Refrigerated" },
                              "items": [
                                {
                                  "price": {
                                    "regular": 1.99,
                                    "promo": 1.59,
                                    "regularPerUnitEstimate": 1.99,
                                    "promoPerUnitEstimate": 1.59
                                  },
                                  "nationalPrice": {
                                    "regular": 2.29,
                                    "promo": 1.89,
                                    "regularPerUnitEstimate": 2.29,
                                    "promoPerUnitEstimate": 1.89
                                  }
                                }
                              ]
                            }
                          ]
                        }
                        """));

        KrogerProductsHttpClient client = new KrogerProductsHttpClient(
                properties(),
                authClient,
                new ObjectMapper()
        );

        ProductsResponse response = client.getProducts("Kroger", "milk", "01400433", 5);

        assertThat(response).isNotNull();
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().productId()).isEqualTo("0001111041729");
        assertThat(response.data().getFirst().temperature().indicator()).isEqualTo("Refrigerated");
        assertThat(response.data().getFirst().items().getFirst().price().regular()).isEqualTo(1.99);
        assertThat(response.data().getFirst().items().getFirst().nationalPrice().regular()).isEqualTo(2.29);

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).contains("/products?");
        assertThat(request.getPath()).contains("filter.brand=Kroger");
        assertThat(request.getPath()).contains("filter.term=milk");
        assertThat(request.getPath()).contains("filter.locationId=01400433");
        assertThat(request.getPath()).contains("filter.limit=5");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer access-token");
    }

    @Test
    void getProductsOmitsOptionalParamsWhenBrandOrLimitMissing() throws Exception {
        when(authClient.getAccessToken()).thenReturn("access-token");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"data\":[]}"));

        KrogerProductsHttpClient client = new KrogerProductsHttpClient(
                properties(),
                authClient,
                new ObjectMapper()
        );

        client.getProducts(" ", "milk", "01400433", null);

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).contains("filter.term=milk");
        assertThat(request.getPath()).contains("filter.locationId=01400433");
        assertThat(request.getPath()).doesNotContain("filter.brand=");
        assertThat(request.getPath()).doesNotContain("filter.limit=");
    }

    @Test
    void getProductsThrowsWhenEndpointReturnsError() throws Exception {
        when(authClient.getAccessToken()).thenReturn("access-token");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"server_error\"}"));

        KrogerProductsHttpClient client = new KrogerProductsHttpClient(
                properties(),
                authClient,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> client.getProducts("Kroger", "milk", "01400433", 5))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Products call failed: HTTP 500");
    }

    private KrogerProperties properties() {
        String baseUrl = mockWebServer.url("/").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        KrogerProperties properties = new KrogerProperties();
        properties.setBaseUrl(baseUrl);
        properties.setOauthTokenPath("/token");
        properties.setLocationsPath("/locations");
        properties.setProductsPath("/products");
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setScope("product.compact");
        return properties;
    }
}
