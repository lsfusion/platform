package lsfusion.server.logics.form.interactive.action.async;

import java.util.List;

public class InputListAction {
    public String action;
    public List<QuickAccess> quickAccessList;

    public InputListAction(String action, List<QuickAccess> quickAccessList) {
        this.action = action;
        this.quickAccessList = quickAccessList;
    }
}