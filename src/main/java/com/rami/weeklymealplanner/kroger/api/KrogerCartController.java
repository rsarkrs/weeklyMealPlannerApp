package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.rami.weeklymealplanner.kroger.application.AddItemsToKrogerCartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/kroger/cart")
public class KrogerCartController {

    private final AddItemsToKrogerCartService addItemsService;

    public KrogerCartController(AddItemsToKrogerCartService addItemsService) {
        this.addItemsService = addItemsService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItems(@Valid @RequestBody CartAddRequest request) {
        try {
            JsonNode response = addItemsService.addItems(request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to add cart items", "message", e.getMessage()));
        }
    }
}
