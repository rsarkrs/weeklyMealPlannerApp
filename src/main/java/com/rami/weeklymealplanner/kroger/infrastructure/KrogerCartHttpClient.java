package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.api.CartAddRequest;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class KrogerCartHttpClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final KrogerProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public KrogerCartHttpClient(KrogerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public JsonNode addItems(String userAccessToken, CartAddRequest requestPayload) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(requestPayload);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(properties.getBaseUrl() + properties.getCartAddPath())
                .put(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + userAccessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Cart add call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
            }
            return objectMapper.readTree(body);
        }
    }
}
