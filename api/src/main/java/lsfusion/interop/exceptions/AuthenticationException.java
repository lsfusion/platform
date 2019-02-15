package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class AuthenticationException extends RemoteMessageException {

    public AuthenticationException() {
        super(getString("exceptions.user.must.be.authenticated"));
    }
}
