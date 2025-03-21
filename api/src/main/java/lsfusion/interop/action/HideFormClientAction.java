package lsfusion.interop.action;

public class HideFormClientAction extends ExecuteClientAction {

    public HideFormClientAction() {
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
