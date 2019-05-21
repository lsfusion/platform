package lsfusion.gwt.client.base.exception;

import net.customware.gwt.dispatch.shared.DispatchException;

public class AuthenticationDispatchException extends RemoteMessageDispatchException {

    public AuthenticationDispatchException() {
    }

    public AuthenticationDispatchException(String message) {
        super(message);
    }
}
