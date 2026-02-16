package com.rami.weeklymealplanner.kroger.infrastructure;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.rami.weeklymealplanner.kroger.domain.OAuthTokenResponse;

@Component
public class KrogerUserTokenStore {

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Instant expiresAt;

    public void save(OAuthTokenResponse tokenResponse) {
        if (tokenResponse == null) {
            return;
        }

        if (tokenResponse.accessToken() != null && !tokenResponse.accessToken().isBlank()) {
            this.accessToken = tokenResponse.accessToken();
        }
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()) {
            this.refreshToken = tokenResponse.refreshToken();
        }

        long expiresIn = tokenResponse.expiresIn() != null ? tokenResponse.expiresIn() : 1800L;
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }

    public String getAccessTokenIfValid() {
        if (accessToken == null || expiresAt == null) {
            return null;
        }
        if (Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return accessToken;
        }
        return null;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isConnected() {
        return getAccessTokenIfValid() != null || (refreshToken != null && !refreshToken.isBlank());
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
