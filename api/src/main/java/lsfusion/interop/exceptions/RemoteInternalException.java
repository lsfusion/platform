package lsfusion.interop.exceptions;

import lsfusion.base.ExceptionUtils;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {

    public RemoteInternalException(int ID, String message, Throwable cause) {
        super(getString("exceptions.internal.server.error", ID, message + '\n' + ExceptionUtils.getStackTrace(cause)));
    }
}
