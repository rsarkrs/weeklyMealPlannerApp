package com.rami.weeklymealplanner.kroger.api;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.rami.weeklymealplanner.kroger.application.KrogerUserAuthService;

@RestController
@RequestMapping("/api/v1/kroger/oauth")
public class KrogerOAuthController {

    private final KrogerUserAuthService userAuthService;

    public KrogerOAuthController(KrogerUserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @GetMapping("/authorize-url")
    public ResponseEntity<AuthorizeUrlResponse> getAuthorizeUrl() {
        return ResponseEntity.ok(userAuthService.createAuthorizeUrl());
    }

    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        try {
            userAuthService.handleCallback(code, state);
            return new RedirectView("/?oauth=connected");
        } catch (IllegalArgumentException e) {
            String message = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return new RedirectView("/?oauth=error&message=" + message);
        } catch (IOException e) {
            String message = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return new RedirectView("/?oauth=error&message=" + message);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<OAuthStatusResponse> status() {
        return ResponseEntity.ok(userAuthService.getStatus());
    }
}
