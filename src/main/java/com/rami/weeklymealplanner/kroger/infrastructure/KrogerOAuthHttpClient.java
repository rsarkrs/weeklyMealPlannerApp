package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.domain.OAuthTokenResponse;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class KrogerOAuthHttpClient {

    private final KrogerProperties properties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public KrogerOAuthHttpClient(KrogerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public String buildAuthorizeUrl(String state) {
        HttpUrl url = HttpUrl.parse(properties.getBaseUrl() + properties.getAuthorizationPath())
                .newBuilder()
                .addQueryParameter("response_type", "code")
                .addQueryParameter("client_id", properties.getClientId())
                .addQueryParameter("redirect_uri", properties.getRedirectUri())
                .addQueryParameter("scope", properties.getUserScope())
                .addQueryParameter("state", state)
                .build();
        return url.toString();
    }

    public OAuthTokenResponse exchangeAuthorizationCode(String code) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", properties.getRedirectUri())
                .build();
        return postTokenForm(formBody, "Authorization code exchange failed");
    }

    public OAuthTokenResponse refreshAccessToken(String refreshToken) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        return postTokenForm(formBody, "Token refresh failed");
    }

    private OAuthTokenResponse postTokenForm(RequestBody formBody, String errorPrefix) throws IOException {
        String basicAuth = Base64.getEncoder()
                .encodeToString((properties.getClientId() + ":" + properties.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(properties.getBaseUrl() + properties.getOauthTokenPath())
                .post(formBody)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Authorization", "Basic " + basicAuth)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException(errorPrefix + ": HTTP " + response.code() + " " + response.message() + " | " + body);
            }
            return objectMapper.readValue(body, OAuthTokenResponse.class);
        }
    }
}
