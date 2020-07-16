package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public class OAuth2Credentials implements Serializable {
    private String redirectUrl = "{baseUrl}/{action}/oauth2/code/{registrationId}";
    private String clientAuthenticationMethod;
    private String registrationId;
    private String scope;
    private String authorizationUri;
    private String tokenUri;
    private String jwkSetUri;
    private String userInfoUri;
    private String userNameAttributeName;
    private String clientName;
    private String clientSecret;
    private String clientId;

    private OAuth2Credentials() {
    }

    public static class Builder {
        private OAuth2Credentials oauth2Credentials;

        public Builder(){
            oauth2Credentials = new OAuth2Credentials();
        }

        public OAuth2Credentials.Builder setRedirectUrl(String redirectUrl){
            oauth2Credentials.redirectUrl = redirectUrl;
            return this;
        }

        public OAuth2Credentials.Builder setClientAuthenticationMethod(String clientAuthenticationMethod){
            oauth2Credentials.clientAuthenticationMethod = clientAuthenticationMethod;
            return this;
        }

        public OAuth2Credentials.Builder setRegistrationId(String registrationId){
            oauth2Credentials.registrationId = registrationId;
            return this;
        }

        public OAuth2Credentials.Builder setScope(String scope){
            oauth2Credentials.scope = scope;
            return this;
        }

        public OAuth2Credentials.Builder setAuthorizationUri(String authorizationUri){
            oauth2Credentials.authorizationUri = authorizationUri;
            return this;
        }

        public OAuth2Credentials.Builder setTokenUri(String tokenUri){
            oauth2Credentials.tokenUri = tokenUri;
            return this;
        }

        public OAuth2Credentials.Builder setJwkSetUri(String jwkSetUri){
            oauth2Credentials.jwkSetUri = jwkSetUri;
            return this;
        }

        public OAuth2Credentials.Builder setUserInfoUri(String userInfoUri){
            oauth2Credentials.userInfoUri = userInfoUri;
            return this;
        }

        public OAuth2Credentials.Builder setUserNameAttributeName(String userNameAttributeName){
            oauth2Credentials.userNameAttributeName = userNameAttributeName;
            return this;
        }

        public OAuth2Credentials.Builder setClientName(String clientName){
            oauth2Credentials.clientName = clientName;
            return this;
        }

        public OAuth2Credentials.Builder setClientSecret(String clientSecret){
            oauth2Credentials.clientSecret = clientSecret;
            return this;
        }

        public OAuth2Credentials.Builder setClientId(String clientId){
            oauth2Credentials.clientId = clientId;
            return this;
        }

        public OAuth2Credentials build(){
            return oauth2Credentials;
        }

    }

    public String getRedirectUrl() {
        return redirectUrl;
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
