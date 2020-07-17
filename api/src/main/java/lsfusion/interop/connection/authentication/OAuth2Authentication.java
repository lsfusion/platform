package lsfusion.interop.connection.authentication;

import java.util.Map;

public class OAuth2Authentication extends Authentication {
    private static final String EMAIL_KEY = "email";
    private static final String LOGIN_KEY = "login";
    private static final String NAME_KEY = "name";

    private final String authSecret;
    private String email;
    private String firstName;
    private String lastName;

    public OAuth2Authentication(String login, String authSecret, Map<String, Object> userInfo) {
        super(login);
        this.authSecret = authSecret;
        fillUserInfo(userInfo);
    }

    private void fillUserInfo(Map<String, Object> userInfo) {
        String email = (String) userInfo.get(EMAIL_KEY);
        String login = (String) userInfo.get(LOGIN_KEY);
        String name = (String) userInfo.get(NAME_KEY);

        if (email != null) {
            this.email = email;
        }
        if (login != null) {
            this.firstName = login;
        }
        if (name != null) {
            String[] s1 = name.split(" ");
            this.firstName = s1[0];
            if (s1.length > 1) {
                this.lastName = s1[1];
            }
        }
    }

    public String getAuthSecret() {
        return this.authSecret;
    }

    public String getEmail() {
        return this.email;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }
}