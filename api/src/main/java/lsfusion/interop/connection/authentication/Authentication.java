package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public abstract class Authentication implements Serializable {

    private final String userName;

    public Authentication(String userName) {
        this.userName = userName;
    }

    public String getAuthSecret() {
        return null;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return null;
    }
}
