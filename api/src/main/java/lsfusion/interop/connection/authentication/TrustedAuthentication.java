package lsfusion.interop.connection.authentication;

public class TrustedAuthentication extends Authentication {

    private final String authSecret;

    public TrustedAuthentication(String login, String authSecret) {
        super(login);
        this.authSecret = authSecret;
    }

    @Override
    public String getAuthSecret() {
        return this.authSecret;
    }
}
