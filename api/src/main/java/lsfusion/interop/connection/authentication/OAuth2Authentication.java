package lsfusion.interop.connection.authentication;

import lsfusion.base.Pair;
import java.util.Map;

public class OAuth2Authentication extends Authentication {
    private static final String EMAIL_KEY = "email";
    private static final String NAME_KEY = "name";
    private static final String YANDEX_NAME = "real_name";
    private static final String YANDEX_EMAIL = "default_email";

    private final String authSecret;
    private final String email;
    private String firstName;
    private String lastName;
    private final Map<String, Object> additionalInfo;

    public OAuth2Authentication(String login, String authSecret, Map<String, Object> userInfo) {
        super(login);
        this.authSecret = authSecret;
        this.additionalInfo = userInfo;

        String email = (String) userInfo.get(EMAIL_KEY);
        String name = (String) userInfo.get(NAME_KEY);
        String yandex_email = (String) userInfo.get(YANDEX_EMAIL);
        String yandex_name = (String) userInfo.get(YANDEX_NAME);

        this.email = email != null ? email : yandex_email;

        Pair<String, String> names = name != null ? splitName(name) : yandex_name != null ? splitName(yandex_name) : null;
        if (names != null) {
            this.firstName = names.first;
            this.lastName = names.second;
        }
    }

    private Pair<String, String> splitName(String name) {
        String[] names = name.split(" ");
        return new Pair<>(names[0], names.length > 1 ? names[1] : null);
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
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