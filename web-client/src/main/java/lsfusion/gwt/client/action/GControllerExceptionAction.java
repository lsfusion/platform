package lsfusion.gwt.client.action;

// a form-controller exec/eval/change ERROR -> rejects the JS callback (cancelled marks an interactive cancel)
public class GControllerExceptionAction extends GControllerCallbackAction {
    public String message;
    public boolean cancelled;

    @SuppressWarnings("UnusedDeclaration")
    public GControllerExceptionAction() {}

    public GControllerExceptionAction(String message, boolean cancelled) {
        this.message = message;
        this.cancelled = cancelled;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
