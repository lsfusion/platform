package lsfusion.server.logics.form.interactive.action.async;

import java.util.List;

public class InputListAction {
    public String action;
    public AsyncExec asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public List<QuickAccess> quickAccessList;

    public InputListAction(String action, AsyncExec asyncExec, List<QuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.quickAccessList = quickAccessList;
    }
}