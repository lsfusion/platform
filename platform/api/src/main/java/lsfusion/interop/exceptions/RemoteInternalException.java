package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {
    public String trace;

    public RemoteInternalException(int ID, String message, String trace) {
        super(getString("exceptions.internal.server.error", ID, message));
        this.trace = trace;
    }
}
