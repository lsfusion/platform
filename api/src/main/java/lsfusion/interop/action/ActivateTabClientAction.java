package lsfusion.interop.action;

import java.io.IOException;

public class ActivateTabClientAction extends ExecuteClientAction {

    public String formSID;
    public String tabSID;

    public ActivateTabClientAction(String formSID, String tabSID) {
        this.formSID = formSID;
        this.tabSID = tabSID;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "ActiveTabClientAction[form:" + formSID + ", tabSID " + tabSID + "]";
    }
}