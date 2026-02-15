package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class KrogerAuthHttpClient {

    private final KrogerProperties properties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private volatile String cachedAccessToken;
    private volatile Instant expiresAt;

    public KrogerAuthHttpClient(KrogerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public String getAccessToken() throws IOException {
        Instant now = Instant.now();
        if (cachedAccessToken != null && expiresAt != null && now.isBefore(expiresAt.minusSeconds(60))) {
            return cachedAccessToken;
        }

        lock.lock();
        try {
            now = Instant.now();
            if (cachedAccessToken != null && expiresAt != null && now.isBefore(expiresAt.minusSeconds(60))) {
                return cachedAccessToken;
            }

            String basicAuth = Base64.getEncoder()
                    .encodeToString((properties.getClientId() + ":" + properties.getClientSecret())
                            .getBytes(StandardCharsets.UTF_8));

            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("scope", properties.getScope())
                    .build();

            Request tokenRequest = new Request.Builder()
                    .url(properties.getBaseUrl() + properties.getOauthTokenPath())
                    .post(formBody)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Authorization", "Basic " + basicAuth)
                    .build();

            try (Response response = httpClient.newCall(tokenRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    throw new IOException("Token call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
                }

                JsonNode json = objectMapper.readTree(body);
                String accessToken = json.path("access_token").asText();
                int expiresIn = Objects.requireNonNullElse(json.path("expires_in").asInt(1800), 1800);

                if (isBlank(accessToken)) {
                    throw new IOException("Token response missing access_token: " + body);
                }

                this.cachedAccessToken = accessToken;
                this.expiresAt = Instant.now().plusSeconds(expiresIn);

                return accessToken;
            }
        } finally {
            lock.unlock();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
