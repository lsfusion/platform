package lsfusion.gwt.shared.action;

public class GEditNotPerformedAction extends GExecuteAction {
    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
