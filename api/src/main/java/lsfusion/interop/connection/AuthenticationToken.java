package lsfusion.interop.connection;

import java.io.Serializable;

public class AuthenticationToken implements Serializable {

    // current implementation - jwt global signed stateless token
    public final String string;
    
    public static final AuthenticationToken ANONYMOUS = new AuthenticationToken("anonymous");
    
    public boolean isAnonymous() {
        return string.equals("anonymous");
    }

    public AuthenticationToken(String string) {
        this.string = string;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof AuthenticationToken && string.equals(((AuthenticationToken) o).string);
    }

    public int hashCode() {
        return string.hashCode();
    }
}
