package lsfusion.interop.exceptions;

import static lsfusion.base.ApiResourceBundle.getString;

public class RemoteInternalException extends RemoteServerException {

    public final String lsfStack;
    
    public RemoteInternalException(String message, String lsfStack) {
        super(getString("exceptions.internal.server.error", message));
        
        this.lsfStack = lsfStack;
    }
}
