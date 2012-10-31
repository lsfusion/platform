package platform.interop.exceptions;

import static platform.base.ApiResourceBundle.getString;

public class LoginException extends RemoteMessageException {
    public LoginException() {
        super(getString("exceptions.incorrect.user.name.or.password"));
    }
}
