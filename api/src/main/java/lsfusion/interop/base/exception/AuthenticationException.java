package lsfusion.interop.base.exception;

import static lsfusion.base.ApiResourceBundle.getString;

public class AuthenticationException extends RemoteMessageException {
    public boolean redirect;

    public AuthenticationException(String message) {
        this(message, false);
    }

    public AuthenticationException(String message, boolean redirect) {
        super(message);

        this.redirect = redirect;
    }
}
