package lsfusion.interop.action;

public class CloseFormClientAction extends ExecuteClientAction {
    // the address CLOSE FORM named: any part may be absent, and an absent part does not narrow it - every form the
    // address reaches is closed
    public String formId;
    public String formCanonicalName;
    public String windowCanonicalName;

    public CloseFormClientAction(String formId, String formCanonicalName, String windowCanonicalName) {
        this.formId = formId;
        this.formCanonicalName = formCanonicalName;
        this.windowCanonicalName = windowCanonicalName;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
