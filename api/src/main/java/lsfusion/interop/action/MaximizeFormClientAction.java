package lsfusion.interop.action;

public class MaximizeFormClientAction extends ExecuteClientAction {

    public MaximizeFormClientAction() {
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}