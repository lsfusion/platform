package lsfusion.interop.action;

public class ActivateFormClientAction extends ExecuteClientAction {
    public String formCanonicalName;

    public ActivateFormClientAction(String formCanonicalName) {
        this.formCanonicalName = formCanonicalName;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}