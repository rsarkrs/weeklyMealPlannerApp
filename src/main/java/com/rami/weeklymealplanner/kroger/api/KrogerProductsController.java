package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rami.weeklymealplanner.kroger.application.FindKrogerProductsService;

@RestController
@RequestMapping("/api/v1/kroger")
public class KrogerProductsController {

    private final FindKrogerProductsService productsService;

    public KrogerProductsController(FindKrogerProductsService productsService) {
        this.productsService = productsService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(
            @RequestParam String term,
            @RequestParam String locationId,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "10") int limit
    ) {
        if (term.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "term is required"));
        }
        if (locationId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "locationId is required"));
        }
        if (limit <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "limit must be > 0"));
        }

        try {
            List<ProductSummaryResponse> results = productsService.find(term, locationId, brand, limit);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch products", "message", e.getMessage()));
        }
    }
}
