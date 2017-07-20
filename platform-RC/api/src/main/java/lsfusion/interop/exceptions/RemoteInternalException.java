package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {

    public RemoteInternalException(int ID, String message) {
        super(getString("exceptions.internal.server.error", ID, message));
    }
}
