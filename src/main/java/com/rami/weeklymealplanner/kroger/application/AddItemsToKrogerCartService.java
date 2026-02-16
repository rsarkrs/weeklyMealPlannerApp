package com.rami.weeklymealplanner.kroger.application;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rami.weeklymealplanner.kroger.api.CartAddItemRequest;
import com.rami.weeklymealplanner.kroger.api.CartAddRequest;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerCartHttpClient;

@Service
public class AddItemsToKrogerCartService {

    private final KrogerUserAuthService userAuthService;
    private final KrogerCartHttpClient cartClient;

    public AddItemsToKrogerCartService(
            KrogerUserAuthService userAuthService,
            KrogerCartHttpClient cartClient
    ) {
        this.userAuthService = userAuthService;
        this.cartClient = cartClient;
    }

    public JsonNode addItems(CartAddRequest request) throws IOException {
        String userToken = userAuthService.getValidUserAccessToken();
        CartAddRequest normalized = normalizeRequest(request);
        return cartClient.addItems(userToken, normalized);
    }

    private CartAddRequest normalizeRequest(CartAddRequest request) {
        List<CartAddItemRequest> normalizedItems = request.items().stream()
                .map(item -> new CartAddItemRequest(
                        item.upc(),
                        item.quantity(),
                        normalizeModality(item.modality())
                ))
                .toList();
        return new CartAddRequest(normalizedItems);
    }

    private String normalizeModality(String modality) {
        if (modality == null || modality.isBlank()) {
            return "PICKUP";
        }
        return modality.trim().toUpperCase();
    }
}
