package lsfusion.gwt.shared.action;

public class GThrowExceptionAction extends GExecuteAction {
    public Throwable throwable;

    @SuppressWarnings("UnusedDeclaration")
    public GThrowExceptionAction() {}

    public GThrowExceptionAction(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        throw throwable;
    }
}
