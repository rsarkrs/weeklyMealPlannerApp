package com.rami.weeklymealplanner.kroger.application;

import java.io.IOException;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.rami.weeklymealplanner.config.KrogerProperties;
import com.rami.weeklymealplanner.kroger.api.AuthorizeUrlResponse;
import com.rami.weeklymealplanner.kroger.api.OAuthStatusResponse;
import com.rami.weeklymealplanner.kroger.domain.OAuthTokenResponse;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerOAuthHttpClient;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerOAuthStateStore;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerUserTokenStore;

@Service
public class KrogerUserAuthService {

    private final KrogerProperties properties;
    private final KrogerOAuthHttpClient oauthClient;
    private final KrogerOAuthStateStore stateStore;
    private final KrogerUserTokenStore tokenStore;

    public KrogerUserAuthService(
            KrogerProperties properties,
            KrogerOAuthHttpClient oauthClient,
            KrogerOAuthStateStore stateStore,
            KrogerUserTokenStore tokenStore
    ) {
        this.properties = properties;
        this.oauthClient = oauthClient;
        this.stateStore = stateStore;
        this.tokenStore = tokenStore;
    }

    public AuthorizeUrlResponse createAuthorizeUrl() {
        String state = stateStore.issueState();
        String url = oauthClient.buildAuthorizeUrl(state);
        return new AuthorizeUrlResponse(url, state, properties.getUserScope());
    }

    public void handleCallback(String code, String state) throws IOException {
        if (!stateStore.consumeIfValid(state)) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }
        OAuthTokenResponse token = oauthClient.exchangeAuthorizationCode(code);
        tokenStore.save(token);
    }

    public String getValidUserAccessToken() throws IOException {
        String token = tokenStore.getAccessTokenIfValid();
        if (token != null) {
            return token;
        }

        String refreshToken = tokenStore.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException("No user token available. Connect your Kroger account first.");
        }

        OAuthTokenResponse refreshed = oauthClient.refreshAccessToken(refreshToken);
        tokenStore.save(refreshed);
        String refreshedToken = tokenStore.getAccessTokenIfValid();
        if (refreshedToken == null) {
            throw new IllegalStateException("Failed to refresh user access token");
        }
        return refreshedToken;
    }

    public OAuthStatusResponse getStatus() {
        Instant expiresAt = tokenStore.getExpiresAt();
        return new OAuthStatusResponse(
                tokenStore.isConnected(),
                expiresAt != null ? expiresAt.toString() : null
        );
    }
}
