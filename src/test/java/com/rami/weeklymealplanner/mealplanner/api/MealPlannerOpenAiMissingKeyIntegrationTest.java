package com.rami.weeklymealplanner.mealplanner.api;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "openai.base-url=https://api.openai.com",
                "openai.api-key=",
                "openai.model=gpt-4.1-mini",
                "kroger.client-id=integration-client-id",
                "kroger.client-secret=integration-client-secret"
        }
)
class MealPlannerOpenAiMissingKeyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void generateEndpointReturnsInternalServerErrorWhenApiKeyMissing() {
        ResponseEntity<String> response = restTemplate.exchange(
                localUrl("/api/v1/meal-planner/generate"),
                HttpMethod.POST,
                requestEntity(sampleRequestBody()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).contains("OPENAI_API_KEY is missing");
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
                    }
                  ],
                  "preferences": {
                    "proteins": ["chicken breast"],
                    "veggies": ["broccoli"],
                    "carbs": ["potato"]
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

    private String localUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
