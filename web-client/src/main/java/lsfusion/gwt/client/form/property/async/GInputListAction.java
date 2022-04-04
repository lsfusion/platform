package lsfusion.gwt.client.form.property.async;

import java.io.Serializable;
import java.util.ArrayList;

public class GInputListAction implements Serializable {
    public String action;
    public GAsyncExec asyncExec;
    public ArrayList<GQuickAccess> quickAccessList;

    @SuppressWarnings("unused")
    public GInputListAction() {
    }

    public GInputListAction(String action, GAsyncExec asyncExec, ArrayList<GQuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.quickAccessList = quickAccessList;
    }
}