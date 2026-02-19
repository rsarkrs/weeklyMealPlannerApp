package com.rami.weeklymealplanner.mealplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MealPlannerOpenAiApiIntegrationTest {

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
        registry.add("openai.base-url", MealPlannerOpenAiApiIntegrationTest::mockBaseUrl);
        registry.add("openai.api-key", () -> "integration-openai-key");
        registry.add("openai.model", () -> "gpt-4.1-mini");
        registry.add("kroger.client-id", () -> "integration-client-id");
        registry.add("kroger.client-secret", () -> "integration-client-secret");
    }

    @BeforeEach
    void clearRecordedRequests() throws InterruptedException {
        while (mockWebServer.takeRequest(25, TimeUnit.MILLISECONDS) != null) {
            // Drain previous test requests so assertions only inspect requests from current test.
        }
    }

    @Test
    void generateEndpointReturnsAiWrappedPayloadWhenOpenAiSucceeds() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("""
                        {
                          "id": "chatcmpl_123",
                          "object": "chat.completion",
                          "choices": [
                            {
                              "index": 0,
                              "message": {
                                "role": "assistant",
                                "content": "{\\"plans\\":[],\\"shoppingListRaw\\":[],\\"notes\\":[\\"ok\\"]}"
                              }
                            }
                          ]
                        }
                        """));

        ResponseEntity<String> response = restTemplate.exchange(
                localUrl("/api/v1/meal-planner/generate"),
                HttpMethod.POST,
                requestEntity(sampleRequestBody()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode payload = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(payload.path("provider").asText()).isEqualTo("openai");
        assertThat(payload.path("model").asText()).isEqualTo("gpt-4.1-mini");
        assertThat(payload.path("generatedPlan").path("notes").get(0).asText()).isEqualTo("ok");
        assertThat(payload.path("input").path("people")).hasSize(2);

        RecordedRequest openAiRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(openAiRequest).isNotNull();
        assertThat(openAiRequest.getPath()).isEqualTo("/v1/chat/completions");
        assertThat(openAiRequest.getHeader("Authorization")).isEqualTo("Bearer integration-openai-key");
        assertThat(openAiRequest.getBody().readUtf8()).contains("\"model\":\"gpt-4.1-mini\"");
    }

    @Test
    void generateEndpointReturnsInternalServerErrorWhenOpenAiFails() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"message\":\"invalid api key\"}}"));

        ResponseEntity<String> response = restTemplate.exchange(
                localUrl("/api/v1/meal-planner/generate"),
                HttpMethod.POST,
                requestEntity(sampleRequestBody()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("Failed to generate weekly plan with OpenAI");
        assertThat(response.getBody()).contains("OpenAI call failed: HTTP 401");
    }

    private static HttpEntity<String> requestEntity(String requestJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(requestJson, headers);
    }

    private static String sampleRequestBody() {
        return """
                {
                  "people": [
                    {
                      "personId": "profile-p1",
                      "sex": "MALE",
                      "ageYears": 30,
                      "heightInches": 73,
                      "weightLbs": 320,
                      "targetLossLbsPerWeek": 1
                    },
                    {
                      "personId": "profile-p2",
                      "sex": "FEMALE",
                      "ageYears": 30,
                      "heightInches": 61,
                      "weightLbs": 160,
                      "targetLossLbsPerWeek": 0.5
                    }
                  ],
                  "preferences": {
                    "proteins": ["chicken breast", "eggs", "ground beef", "ground chicken"],
                    "veggies": ["bell peppers", "broccoli"],
                    "carbs": ["pinto beans", "potato", "sweet potato"]
                  },
                  "allergies": {
                    "excludedIngredients": []
                  },
                  "optimizationGoal": "BALANCED",
                  "days": 7,
                  "mealsPerDay": 3,
                  "maxPrepMinutesPerMeal": 20,
                  "maxCookMinutesPerMeal": 30
                }
                """;
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
