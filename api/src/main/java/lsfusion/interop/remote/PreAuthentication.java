package lsfusion.interop.remote;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

public class PreAuthentication implements Serializable {
    public final List<String> roles;
    public final Locale locale;

    public PreAuthentication(List<String> roles, Locale locale) {
        this.roles = roles;
        this.locale = locale;
    }
}
