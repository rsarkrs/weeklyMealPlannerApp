package com.rami.weeklymealplanner.kroger.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CartAddItemRequest(
        @NotBlank String upc,
        @NotNull @Min(1) Integer quantity,
        String modality
) {}
