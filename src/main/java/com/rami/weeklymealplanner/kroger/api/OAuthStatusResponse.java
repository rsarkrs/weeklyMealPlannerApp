package com.rami.weeklymealplanner.kroger.api;

public record OAuthStatusResponse(
        boolean connected,
        String expiresAt
) {}
