package lsfusion.gwt.base.shared;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;

public class InvalidateException extends DispatchException {
    public Action action;

    public InvalidateException() {
    }

    public InvalidateException(Throwable cause) {
        super(cause);
    }

    public InvalidateException(String message) {
        super(message);
    }

    public InvalidateException(Action action, String message) {
        super(message);
        this.action = action;
    }

    public InvalidateException(String message, Throwable cause) {
        super(message, cause);
    }
}
