package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rami.weeklymealplanner.kroger.application.FindKrogerLocationsService;

@RestController
@RequestMapping("/api/v1/kroger")
public class KrogerController {

    private final FindKrogerLocationsService locationsService;

    public KrogerController(FindKrogerLocationsService locationsService) {
        this.locationsService = locationsService;
    }

    @GetMapping("/locations")
    public ResponseEntity<?> getLocations(
            @RequestParam String zip,
            @RequestParam(defaultValue = "10") int radius,
            @RequestParam(defaultValue = "5") int limit
    ) {
        if (zip.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "zip is required"));
        }
        if (radius <= 0 || limit <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "radius and limit must be > 0"));
        }

        try {
            List<LocationSummaryResponse> results = locationsService.find(zip, radius, limit);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch locations", "message", e.getMessage()));
        }
    }
}
