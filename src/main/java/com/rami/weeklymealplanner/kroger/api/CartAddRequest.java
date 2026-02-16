package com.rami.weeklymealplanner.kroger.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CartAddRequest(
        @NotEmpty @Valid List<CartAddItemRequest> items
) {}
