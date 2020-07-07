package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public class OAuth2Credentials implements Serializable {
    private final String clientId;
    private final String clientSecret;

    public OAuth2Credentials(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
