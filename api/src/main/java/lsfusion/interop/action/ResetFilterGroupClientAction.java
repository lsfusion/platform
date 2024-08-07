package lsfusion.interop.action;

public class ResetFilterGroupClientAction extends ExecuteClientAction {

    public String sid;

    public ResetFilterGroupClientAction(String sid) {
        this.sid = sid;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}