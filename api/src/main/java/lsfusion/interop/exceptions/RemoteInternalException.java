package lsfusion.interop.exceptions;

import lsfusion.base.ExceptionUtils;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {

    public final String javaStack;
    public final String lsfStack;

    public RemoteInternalException(String message, Throwable cause, String lsfStack) {
        super(message, cause);

        this.javaStack = ExceptionUtils.getStackTrace(cause);
        this.lsfStack = lsfStack;
    }
}
