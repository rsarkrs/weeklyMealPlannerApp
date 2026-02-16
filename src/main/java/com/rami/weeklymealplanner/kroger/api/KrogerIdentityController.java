package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rami.weeklymealplanner.kroger.application.GetKrogerIdentityProfileService;
import com.rami.weeklymealplanner.kroger.domain.IdentityProfileResponse;

@RestController
@RequestMapping("/api/v1/kroger/identity")
public class KrogerIdentityController {

    private final GetKrogerIdentityProfileService identityService;

    public KrogerIdentityController(GetKrogerIdentityProfileService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            IdentityProfileResponse profile = identityService.getProfile();
            return ResponseEntity.ok(profile);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch profile", "message", e.getMessage()));
        }
    }
}
