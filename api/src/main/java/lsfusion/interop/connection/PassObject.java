package lsfusion.interop.connection;

import java.io.Serializable;

public class PassObject implements Serializable {

    private final String password;
    private final String authSecret;

    public PassObject(String password, String authSecret) {
        this.password = password;
        this.authSecret= authSecret;
    }

    public String getPassword() {
        return password;
    }

    public String getAuthSecret() {
        return authSecret;
    }
}
