package com.rami.weeklymealplanner.mealplanner.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rami.weeklymealplanner.config.OpenAiProperties;
import com.rami.weeklymealplanner.mealplanner.api.GeneratePlanRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OpenAiMealPlanHttpClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OpenAiMealPlanHttpClient(OpenAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public JsonNode generate(GeneratePlanRequest request, String prompt) throws IOException {
        if (isBlank(properties.getApiKey())) {
            throw new IllegalStateException("OPENAI_API_KEY is missing. Add it to your environment or .env.");
        }
        if (isBlank(properties.getBaseUrl())) {
            throw new IllegalStateException("OPENAI_BASE_URL is missing.");
        }
        if (isBlank(properties.getModel())) {
            throw new IllegalStateException("OPENAI_MODEL is missing.");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.putObject("response_format").put("type", "json_object");

        ArrayNode messages = body.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "You are a meal planner. Return strict JSON only.");
        messages.addObject()
                .put("role", "user")
                .put("content", prompt + "\n\nInput payload:\n" + objectMapper.writeValueAsString(request));

        String payload = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(payload, JSON_MEDIA_TYPE);

        String baseUrl = properties.getBaseUrl().endsWith("/")
                ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                : properties.getBaseUrl();

        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String rawBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI call failed: HTTP " + response.code() + " " + response.message() + " | " + rawBody);
            }

            JsonNode raw = objectMapper.readTree(rawBody);
            String content = raw.path("choices").path(0).path("message").path("content").asText("");

            ObjectNode out = objectMapper.createObjectNode();
            out.put("provider", "openai");
            out.put("model", properties.getModel());
            out.put("prompt", prompt);
            out.set("input", objectMapper.valueToTree(request));
            out.set("raw", raw);

            if (!content.isBlank()) {
                try {
                    out.set("generatedPlan", objectMapper.readTree(content));
                } catch (IOException ignored) {
                    out.put("generatedText", content);
                }
            }
            return out;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
