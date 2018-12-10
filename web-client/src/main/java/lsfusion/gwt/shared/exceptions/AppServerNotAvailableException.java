package lsfusion.gwt.shared.exceptions;

import net.customware.gwt.dispatch.shared.DispatchException;

public class AppServerNotAvailableException extends DispatchException {

    public AppServerNotAvailableException() {
        super("Application server is not available"); // exceptions.app.server.not.available - Сервер приложений не доступен
    }
}
