package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public class OAuth2Credentials implements Serializable {
    private static final String REDIRECT_URL = "{baseUrl}/{action}/oauth2/code/{registrationId}";
    private final String clientAuthenticationMethod;
    private final String registrationId;
    private final String scope;
    private final String authorizationUri;
    private final String tokenUri;
    private final String jwkSetUri;
    private final String userInfoUri;
    private final String userNameAttributeName;
    private final String clientName;
    private final String clientSecret;
    private final String clientId;

    public OAuth2Credentials(String clientAuthenticationMethod, String registrationId,
                             String scope, String authorizationUri, String tokenUri, String jwkSetUri,
                             String userInfoUri, String userNameAttributeName, String clientName,
                             String clientSecret, String clientId){
        this.clientAuthenticationMethod = clientAuthenticationMethod;
        this.registrationId = registrationId;
        this.scope = scope;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.jwkSetUri = jwkSetUri;
        this.userInfoUri = userInfoUri;
        this.userNameAttributeName = userNameAttributeName;
        this.clientName = clientName;
        this.clientSecret = clientSecret;
        this.clientId = clientId;
    }

    public String getRedirectUrl() {
        return REDIRECT_URL;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getScope() {
        return scope;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }
}
