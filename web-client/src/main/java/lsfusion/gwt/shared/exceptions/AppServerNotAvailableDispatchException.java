package lsfusion.gwt.shared.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class AppServerNotAvailableDispatchException extends DispatchException {

    public AppServerNotAvailableDispatchException(String message) {
        super(message); // exceptions.app.server.not.available
    }
}
