package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public abstract class Authentication implements Serializable {

    private final String userName;

    public Authentication(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }
}
