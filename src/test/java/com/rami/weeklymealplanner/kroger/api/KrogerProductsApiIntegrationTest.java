package com.rami.weeklymealplanner.kroger.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KrogerProductsApiIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static MockWebServer mockWebServer;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterAll
    static void stopServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        ensureServerStarted();
        registry.add("kroger.base-url", KrogerProductsApiIntegrationTest::mockBaseUrl);
        registry.add("kroger.oauth-token-path", () -> "/token");
        registry.add("kroger.locations-path", () -> "/locations");
        registry.add("kroger.products-path", () -> "/products");
        registry.add("kroger.client-id", () -> "integration-client-id");
        registry.add("kroger.client-secret", () -> "integration-client-secret");
        registry.add("kroger.scope", () -> "product.compact");
    }

    @BeforeEach
    void clearRecordedRequests() throws InterruptedException {
        while (mockWebServer.takeRequest(25, TimeUnit.MILLISECONDS) != null) {
            // Drain previous test requests so assertions only inspect requests from current test.
        }
    }

    @Test
    void productsEndpointReturnsMappedResponseWhenUpstreamCallsSucceed() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"it-token\",\"expires_in\":1800}"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "data": [
                            {
                              "productId": "0001111041729",
                              "description": "Kroger 2% Milk",
                              "brand": "Kroger",
                              "items": [
                                {
                                  "upc": "0001111041729",
                                  "size": "1 gal",
                                  "soldBy": "UNIT",
                                  "price": {
                                    "regular": 3.99,
                                    "promo": 2.99
                                  },
                                  "itemInformation": {
                                    "temperature": {
                                      "indicator": "REFRIGERATED"
                                    }
                                  }
                                }
                              ]
                            }
                          ]
                        }
                        """));

        ResponseEntity<String> response = restTemplate.getForEntity(
                localUrl("/api/v1/kroger/products?term=milk&locationId=01400433&brand=Kroger&limit=2"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode payload = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(payload.isArray()).isTrue();
        assertThat(payload).hasSize(1);
        assertThat(payload.get(0).path("productId").asText()).isEqualTo("0001111041729");
        assertThat(payload.get(0).path("brand").asText()).isEqualTo("Kroger");
        assertThat(payload.get(0).path("promoPrice").asDouble()).isEqualTo(2.99);

        RecordedRequest tokenRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest productsRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(tokenRequest).isNotNull();
        assertThat(productsRequest).isNotNull();
        List<String> paths = List.of(tokenRequest.getPath(), productsRequest.getPath());
        assertThat(paths).anySatisfy(path -> assertThat(path).contains("/token"));
        assertThat(paths).anySatisfy(path -> assertThat(path).contains("/products?"));
        assertThat(paths).anySatisfy(path -> assertThat(path).contains("filter.term=milk"));
        assertThat(paths).anySatisfy(path -> assertThat(path).contains("filter.locationId=01400433"));
    }

    @Test
    void productsEndpointReturnsInternalServerErrorWhenTokenCallFails() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":\"invalid_client\"}"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                localUrl("/api/v1/kroger/products?term=milk&locationId=01400433&limit=2"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Failed to fetch products");
        assertThat(response.getBody()).contains("Token call failed: HTTP 401");
    }

    @Test
    void productsEndpointReturnsBadRequestBeforeCallingUpstreamWhenInputsAreInvalid() {
        int requestCountBefore = mockWebServer.getRequestCount();

        ResponseEntity<String> response = restTemplate.getForEntity(
                localUrl("/api/v1/kroger/products?term=milk&locationId=01400433&limit=0"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("limit must be > 0");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(requestCountBefore);
    }

    private static String mockBaseUrl() {
        String url = mockWebServer.url("/").toString();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static void ensureServerStarted() {
        if (mockWebServer == null) {
            mockWebServer = new MockWebServer();
            try {
                mockWebServer.start();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to start mock web server", e);
            }
        }
    }

    private String localUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
