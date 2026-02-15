package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.domain.LocationsResponse;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class KrogerLocationsHttpClient {

    private final KrogerProperties properties;
    private final KrogerAuthHttpClient authClient;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public KrogerLocationsHttpClient(
            KrogerProperties properties,
            KrogerAuthHttpClient authClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public LocationsResponse getLocations(String zipCode, int radiusMiles, int limit) throws IOException {
        String token = authClient.getAccessToken();

        HttpUrl url = HttpUrl.parse(properties.getBaseUrl() + properties.getLocationsPath())
                .newBuilder()
                .addQueryParameter("filter.zipCode.near", zipCode)
                .addQueryParameter("filter.radiusInMiles", String.valueOf(radiusMiles))
                .addQueryParameter("filter.limit", String.valueOf(limit))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Locations call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
            }

            return objectMapper.readValue(body, LocationsResponse.class);
        }
    }
}
