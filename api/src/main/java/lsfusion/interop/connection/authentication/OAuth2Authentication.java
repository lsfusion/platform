package lsfusion.interop.connection.authentication;

import lsfusion.base.Pair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OAuth2Authentication extends Authentication {
    private static final String EMAIL_KEY = "email";
    private static final String NAME_KEY = "name";
    private static final String YANDEX_NAME = "real_name";
    private static final String YANDEX_EMAIL = "default_email";

    private final String authSecret;
    private String email;
    private String firstName;
    private String lastName;
    private Pair<String, String> names;

    public OAuth2Authentication(String login, String authSecret, Map<String, Object> userInfo) {
        super(login);
        this.authSecret = authSecret;
        fillUserInfo(userInfo);
    }

    private void fillUserInfo(Map<String, Object> userInfo) {
        String email = (String) userInfo.get(EMAIL_KEY);
        String name = (String) userInfo.get(NAME_KEY);
        String yandex_email = (String) userInfo.get(YANDEX_EMAIL);
        String yandex_name = (String) userInfo.get(YANDEX_NAME);

        if (email != null) {
            this.email = email;
        } else if (yandex_email != null){
            this.email = yandex_email;
        }

        if (name != null) {
            names = splitName(name);
        } else if (yandex_name != null) {
            names = splitName(yandex_name);
        }

        if (names != null){
            this.firstName = names.first;
            this.lastName = names.second;
        }
    }

    private Pair<String, String> splitName(String name){
        List<String> names = Arrays.asList(name.split(" "));
        if (names.size() > 1){
            return new Pair<>(names.get(0), names.get(1));
        } else {
            return new Pair<>(names.get(0), null);
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