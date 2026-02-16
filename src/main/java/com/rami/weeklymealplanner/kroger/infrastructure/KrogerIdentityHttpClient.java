package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.domain.IdentityProfileResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class KrogerIdentityHttpClient {

    private final KrogerProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public KrogerIdentityHttpClient(KrogerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public IdentityProfileResponse getProfile(String userAccessToken) throws IOException {
        Request request = new Request.Builder()
                .url(properties.getBaseUrl() + properties.getIdentityProfilePath())
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + userAccessToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("Identity profile call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
            }
            return objectMapper.readValue(body, IdentityProfileResponse.class);
        }
    }
}
