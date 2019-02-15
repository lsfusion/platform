package lsfusion.interop.remote;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class AuthenticationToken implements Serializable {

    // current implementation - jwt global signed stateless token
    public final String user;
    
    public static final AuthenticationToken ANONYMOUS = new AuthenticationToken("anonymous");
    
    public boolean isAnonymous() {
        return user.equals("anonymous");
    }

    public AuthenticationToken(String user) {
        this.user = user;
    }
}
