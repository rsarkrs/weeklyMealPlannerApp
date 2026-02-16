package com.rami.weeklymealplanner.kroger.infrastructure;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class KrogerOAuthStateStore {

    private volatile String pendingState;
    private volatile Instant expiresAt;

    public String issueState() {
        this.pendingState = UUID.randomUUID().toString();
        this.expiresAt = Instant.now().plusSeconds(600);
        return pendingState;
    }

    public boolean consumeIfValid(String state) {
        if (state == null || pendingState == null || expiresAt == null) {
            return false;
        }

        boolean valid = pendingState.equals(state) && Instant.now().isBefore(expiresAt);
        if (valid) {
            pendingState = null;
            expiresAt = null;
        }
        return valid;
    }
}
