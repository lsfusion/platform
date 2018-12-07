package lsfusion.gwt.base.shared.exceptions;

import lsfusion.base.ApiResourceBundle;
import net.customware.gwt.dispatch.shared.DispatchException;

public class AppServerNotAvailableException extends DispatchException {

    public AppServerNotAvailableException() {
        super("Application server is not available"); // exceptions.app.server.not.available - Сервер приложений не доступен
    }
}
