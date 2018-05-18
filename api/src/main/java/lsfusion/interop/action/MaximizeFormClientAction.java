package lsfusion.interop.action;

public class MaximizeFormClientAction extends ExecuteClientAction {
    public String formCanonicalName;

    public MaximizeFormClientAction(String formCanonicalName) {
        this.formCanonicalName = formCanonicalName;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}