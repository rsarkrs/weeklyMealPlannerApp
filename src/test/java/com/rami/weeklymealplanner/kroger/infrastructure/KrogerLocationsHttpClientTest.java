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
import com.rami.weeklymealplanner.kroger.domain.LocationsResponse;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(MockitoExtension.class)
class KrogerLocationsHttpClientTest {

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
    void getLocationsSendsExpectedQueryAndAuthorizationAndParsesBody() throws Exception {
        when(authClient.getAccessToken()).thenReturn("access-token");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "data": [
                            {
                              "locationId": "01400433",
                              "chain": "Kroger",
                              "name": "Kroger Marketplace",
                              "phone": "623-555-1212",
                              "address": {
                                "city": "Goodyear",
                                "state": "AZ",
                                "zipCode": "85338"
                              },
                              "hours": {
                                "timezone": "America/Phoenix"
                              },
                              "departments": [
                                {"departmentId": "94"},
                                {"departmentId": "0E"}
                              ]
                            }
                          ],
                          "meta": {"pagination": {"start": 0, "limit": 1, "total": 1}}
                        }
                        """));

        KrogerLocationsHttpClient client = new KrogerLocationsHttpClient(
                properties(),
                authClient,
                new ObjectMapper()
        );

        LocationsResponse response = client.getLocations("85338", 10, 5);

        assertThat(response).isNotNull();
        assertThat(response.data()).hasSize(1);
        assertThat(response.data().getFirst().locationId()).isEqualTo("01400433");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).contains("/locations?");
        assertThat(request.getPath()).contains("filter.zipCode.near=85338");
        assertThat(request.getPath()).contains("filter.radiusInMiles=10");
        assertThat(request.getPath()).contains("filter.limit=5");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer access-token");
    }

    @Test
    void getLocationsThrowsWhenLocationsEndpointReturnsError() throws Exception {
        when(authClient.getAccessToken()).thenReturn("access-token");
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\"error\":\"forbidden\"}"));

        KrogerLocationsHttpClient client = new KrogerLocationsHttpClient(
                properties(),
                authClient,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> client.getLocations("85338", 10, 5))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Locations call failed: HTTP 403");
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
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setScope("product.compact");
        return properties;
    }
}
