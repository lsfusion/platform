package lsfusion.interop.exception;

import static lsfusion.base.ApiResourceBundle.getString;

public class LoginException extends RemoteMessageException {
    public LoginException() {
        super(getString("exceptions.incorrect.user.name.or.password"));
    }
}
