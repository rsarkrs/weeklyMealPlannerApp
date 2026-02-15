package com.rami.weeklymealplanner.kroger.infrastructure;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.domain.ProductsResponse;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
public class KrogerProductsHttpClient {

    private final KrogerProperties properties;
    private final KrogerAuthHttpClient authClient;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public KrogerProductsHttpClient(
            KrogerProperties properties,
            KrogerAuthHttpClient authClient,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.authClient = authClient;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient();
    }

    public ProductsResponse getProducts(String brand, String term, String locationId, Integer limit) throws IOException {
        String token = authClient.getAccessToken();

        HttpUrl.Builder urlBuilder = HttpUrl.parse(properties.getBaseUrl() + properties.getProductsPath())
                .newBuilder()
                .addQueryParameter("filter.term", term)
                .addQueryParameter("filter.locationId", locationId);

        if (!isBlank(brand)) {
            urlBuilder.addQueryParameter("filter.brand", brand.trim());
        }
        if (limit != null && limit > 0) {
            urlBuilder.addQueryParameter("filter.limit", String.valueOf(limit));
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Products call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
            }

            return objectMapper.readValue(body, ProductsResponse.class);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
