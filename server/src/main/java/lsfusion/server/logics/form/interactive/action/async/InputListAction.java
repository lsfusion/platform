package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.base.col.interfaces.immutable.ImList;

import java.util.List;

public class InputListAction {
    public String action;
    public AsyncEventExec asyncExec; // it's an asyncexec and not asynceventexec, since in continueDispatching there is no push infrastructure so far (and it's not clear if it's needed at all)
    public ImList<QuickAccess> quickAccessList;

    public InputListAction(String action, AsyncEventExec asyncExec, ImList<QuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.quickAccessList = quickAccessList;
    }
}