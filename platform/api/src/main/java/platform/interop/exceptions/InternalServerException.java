package platform.interop.exceptions;

import static platform.base.ApiResourceBundle.getString;

public class InternalServerException extends RemoteServerException {
    public String trace;

    public InternalServerException(int ID, String message, String trace) {
        super(getString("exceptions.internal.server.error", ID, message));
        this.trace = trace;
    }
}
