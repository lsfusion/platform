package lsfusion.interop.connection;

import java.io.Serializable;

public class AuthenticationToken implements Serializable {

    // current implementation - jwt global signed stateless token
    public final String string;

    public final boolean use2FA;
    
    public static final AuthenticationToken ANONYMOUS = new AuthenticationToken("anonymous");
    
    public boolean isAnonymous() {
        return string.equals("anonymous");
    }

    public AuthenticationToken(String string) {
        this(string, false);
    }

    public AuthenticationToken(String string, boolean use2FA) {
        this.string = string;
        this.use2FA = use2FA;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof AuthenticationToken && string.equals(((AuthenticationToken) o).string)
                && use2FA == ((AuthenticationToken) o).use2FA;
    }

    public int hashCode() {
        return string.hashCode();
    }
}
