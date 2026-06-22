package lsfusion.interop.action;

/**
 * A form-controller exec/eval/change ERROR delivered to the JS callback's onException (see
 * {@link ControllerCallbackClientAction}). {@code cancelled} marks an interactive cancellation vs an error.
 */
public class ControllerExceptionClientAction extends ControllerCallbackClientAction {

    public final String message;
    public final boolean cancelled;

    public ControllerExceptionClientAction(String message, boolean cancelled) {
        this.message = message;
        this.cancelled = cancelled;
    }
}
