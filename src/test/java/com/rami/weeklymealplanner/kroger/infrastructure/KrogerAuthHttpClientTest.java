package com.rami.weeklymealplanner.kroger.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class KrogerAuthHttpClientTest {

    private MockWebServer mockWebServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    void getAccessTokenReturnsTokenAndSendsExpectedHeadersAndBody() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"token-123\",\"expires_in\":1800}"));

        KrogerAuthHttpClient client = new KrogerAuthHttpClient(properties(), objectMapper);
        String token = client.getAccessToken();

        assertThat(token).isEqualTo("token-123");

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/token");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Authorization")).startsWith("Basic ");
        assertThat(request.getBody().readUtf8())
                .contains("grant_type=client_credentials")
                .contains("scope=product.compact");
    }

    @Test
    void getAccessTokenUsesCacheBeforeExpiry() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"cached-token\",\"expires_in\":1800}"));

        KrogerAuthHttpClient client = new KrogerAuthHttpClient(properties(), objectMapper);

        String first = client.getAccessToken();
        String second = client.getAccessToken();

        assertThat(first).isEqualTo("cached-token");
        assertThat(second).isEqualTo("cached-token");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void getAccessTokenThrowsWhenTokenEndpointReturnsError() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":\"invalid_client\"}"));

        KrogerAuthHttpClient client = new KrogerAuthHttpClient(properties(), objectMapper);

        assertThatThrownBy(client::getAccessToken)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Token call failed: HTTP 401");
    }

    @Test
    void getAccessTokenThrowsWhenAccessTokenMissing() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"token_type\":\"bearer\",\"expires_in\":1800}"));

        KrogerAuthHttpClient client = new KrogerAuthHttpClient(properties(), objectMapper);

        assertThatThrownBy(client::getAccessToken)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Token response missing access_token");
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
