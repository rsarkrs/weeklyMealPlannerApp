package com.rami.weeklymealplanner.kroger.application;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.rami.weeklymealplanner.kroger.domain.IdentityProfileResponse;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerIdentityHttpClient;

@Service
public class GetKrogerIdentityProfileService {

    private final KrogerUserAuthService userAuthService;
    private final KrogerIdentityHttpClient identityClient;

    public GetKrogerIdentityProfileService(
            KrogerUserAuthService userAuthService,
            KrogerIdentityHttpClient identityClient
    ) {
        this.userAuthService = userAuthService;
        this.identityClient = identityClient;
    }

    public IdentityProfileResponse getProfile() throws IOException {
        String userToken = userAuthService.getValidUserAccessToken();
        return identityClient.getProfile(userToken);
    }
}
