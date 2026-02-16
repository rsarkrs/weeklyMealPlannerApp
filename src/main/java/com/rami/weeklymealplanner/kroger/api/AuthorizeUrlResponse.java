package com.rami.weeklymealplanner.kroger.api;

public record AuthorizeUrlResponse(
        String authorizeUrl,
        String state,
        String scope
) {}
