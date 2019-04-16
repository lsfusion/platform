package lsfusion.gwt.client.base.exception;

import net.customware.gwt.dispatch.shared.DispatchException;

public class AppServerNotAvailableDispatchException extends DispatchException {

    public AppServerNotAvailableDispatchException(String message) {
        super(message); // exceptions.app.server.not.available
    }

    //need for serializable
    public AppServerNotAvailableDispatchException() {
    }
}
