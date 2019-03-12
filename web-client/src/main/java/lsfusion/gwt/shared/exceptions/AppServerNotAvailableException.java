package lsfusion.gwt.shared.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class AppServerNotAvailableException extends DispatchException {

    public AppServerNotAvailableException(String message) {
        super(message); // exceptions.app.server.not.available
    }
}
