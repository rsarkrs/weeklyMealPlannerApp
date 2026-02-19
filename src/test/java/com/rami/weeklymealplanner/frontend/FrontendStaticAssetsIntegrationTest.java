package com.rami.weeklymealplanner.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "kroger.client-id=integration-client-id",
                "kroger.client-secret=integration-client-secret"
        }
)
class FrontendStaticAssetsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void indexPageIncludesNewMealPlannerInputsAndRenderTargets() {
        ResponseEntity<String> response = restTemplate.getForEntity(localUrl("/"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("id=\"mealPlanZip\"");
        assertThat(response.getBody()).contains("id=\"optimizationGoal\"");
        assertThat(response.getBody()).contains("id=\"allergiesInput\"");
        assertThat(response.getBody()).contains("id=\"mealPlanCards\"");
        assertThat(response.getBody()).contains("id=\"shoppingListCards\"");
    }

    @Test
    void appJsIncludesAiGenerateFlowAndPayloadFields() {
        ResponseEntity<String> response = restTemplate.getForEntity(localUrl("/app.js"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("/api/v1/meal-planner/generate");
        assertThat(response.getBody()).contains("payload.generatedPlan");
        assertThat(response.getBody()).contains("maxPrepMinutesPerMeal");
        assertThat(response.getBody()).contains("maxCookMinutesPerMeal");
        assertThat(response.getBody()).contains("ageYears");
    }

    @Test
    void stylesIncludeMealPlanRenderingClasses() {
        ResponseEntity<String> response = restTemplate.getForEntity(localUrl("/styles.css"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(".meal-day-grid");
        assertThat(response.getBody()).contains(".meal-block");
        assertThat(response.getBody()).contains(".ingredient-list");
    }

    private String localUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
