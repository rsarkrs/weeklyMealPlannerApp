package com.rami.weeklymealplanner.sandbox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StoreLocations {

  private static final String BASE_URL = "https://api.kroger.com";

  public static void main(String[] args) {
    String clientId = System.getenv("KROGER_CLIENT_ID");
    String clientSecret = System.getenv("KROGER_CLIENT_SECRET");

    if (isBlank(clientId) || isBlank(clientSecret)) {
      System.err.println("Set KROGER_CLIENT_ID and KROGER_CLIENT_SECRET before running.");
      return;
    }

    OkHttpClient client = new OkHttpClient();

    try {
      String accessToken = fetchAccessToken(client, clientId, clientSecret);

      HttpUrl locationsUrl = HttpUrl.parse(BASE_URL + "/v1/locations")
          .newBuilder()
          .addQueryParameter("filter.zipCode.near", "85338")   // change as needed
          .addQueryParameter("filter.radiusInMiles", "10")
          .addQueryParameter("filter.limit", "5")
          .build();

      Request locationsRequest = new Request.Builder()
          .url(locationsUrl)
          .get()
          .addHeader("Accept", "application/json")
          .addHeader("Authorization", "Bearer " + accessToken)
          .build();

      try (Response response = client.newCall(locationsRequest).execute()) {
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
          System.err.println("Locations call failed: HTTP " + response.code() + " " + response.message());
          System.err.println(body);
          return;
        }

        System.out.println(body);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static String fetchAccessToken(OkHttpClient client, String clientId, String clientSecret) throws IOException {
    String basicAuth = Base64.getEncoder()
        .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

    RequestBody formBody = new FormBody.Builder()
        .add("grant_type", "client_credentials")
        .add("scope", "product.compact")
        .build();

    Request tokenRequest = new Request.Builder()
        .url(BASE_URL + "/v1/connect/oauth2/token")
        .post(formBody)
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/x-www-form-urlencoded")
        .addHeader("Authorization", "Basic " + basicAuth)
        .build();

    try (Response response = client.newCall(tokenRequest).execute()) {
      String body = response.body() != null ? response.body().string() : "";

      if (!response.isSuccessful()) {
        throw new IOException("Token call failed: HTTP " + response.code() + " " + response.message() + " | " + body);
      }

      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(body);
      String accessToken = json.path("access_token").asText();

      if (isBlank(accessToken)) {
        throw new IOException("Token response missing access_token: " + body);
      }

      return accessToken;
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
