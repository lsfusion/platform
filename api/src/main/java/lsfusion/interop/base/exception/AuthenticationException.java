package lsfusion.interop.base.exception;

import static lsfusion.base.ApiResourceBundle.getString;

public class AuthenticationException extends RemoteMessageException {
    public boolean redirect;

    // not authenticated
    public AuthenticationException() {
        super(getString("exceptions.user.must.be.authenticated"));
        this.redirect = true;
    }

    // token problems (expiration, invalid, etc)
    public AuthenticationException(String message) {
        super(message);
    }
}
