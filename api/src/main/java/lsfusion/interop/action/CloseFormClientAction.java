package lsfusion.interop.action;

public class CloseFormClientAction extends ExecuteClientAction {
    public String formId;

    public CloseFormClientAction(String formId) {
        this.formId = formId;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}