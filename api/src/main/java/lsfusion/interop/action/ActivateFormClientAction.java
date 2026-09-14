package lsfusion.interop.action;

public class ActivateFormClientAction extends ExecuteClientAction {
    // the address ACTIVATE FORM named: any part may be absent, and an absent part does not narrow it
    public String formCanonicalName;
    public String formId;
    public String windowCanonicalName;

    public ActivateFormClientAction(String formCanonicalName, String formId, String windowCanonicalName) {
        this.formCanonicalName = formCanonicalName;
        this.formId = formId;
        this.windowCanonicalName = windowCanonicalName;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
