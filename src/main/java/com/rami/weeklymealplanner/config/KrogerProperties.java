package com.rami.weeklymealplanner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "kroger")
public class KrogerProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String oauthTokenPath;

    @NotBlank
    private String locationsPath;

    @NotBlank
    private String productsPath;

    @NotBlank
    private String authorizationPath;

    @NotBlank
    private String identityProfilePath;

    @NotBlank
    private String cartAddPath;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String userScope;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String scope;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getOauthTokenPath() {
        return oauthTokenPath;
    }

    public void setOauthTokenPath(String oauthTokenPath) {
        this.oauthTokenPath = oauthTokenPath;
    }

    public String getLocationsPath() {
        return locationsPath;
    }

    public void setLocationsPath(String locationsPath) {
        this.locationsPath = locationsPath;
    }

    public String getProductsPath() {
        return productsPath;
    }

    public void setProductsPath(String productsPath) {
        this.productsPath = productsPath;
    }

    public String getAuthorizationPath() {
        return authorizationPath;
    }

    public void setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = authorizationPath;
    }

    public String getIdentityProfilePath() {
        return identityProfilePath;
    }

    public void setIdentityProfilePath(String identityProfilePath) {
        this.identityProfilePath = identityProfilePath;
    }

    public String getCartAddPath() {
        return cartAddPath;
    }

    public void setCartAddPath(String cartAddPath) {
        this.cartAddPath = cartAddPath;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getUserScope() {
        return userScope;
    }

    public void setUserScope(String userScope) {
        this.userScope = userScope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
