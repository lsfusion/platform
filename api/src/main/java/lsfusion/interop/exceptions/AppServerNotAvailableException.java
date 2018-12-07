package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class AppServerNotAvailableException extends RemoteMessageException {
    public AppServerNotAvailableException() {
        super(getString("exceptions.app.server.not.available"));
    }
}
