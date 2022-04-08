package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.event.GKeyStroke;

import java.io.Serializable;
import java.util.ArrayList;

public class GInputListAction implements Serializable {
    public String action;
    public GAsyncEventExec asyncExec;
    public GKeyStroke keyStroke;
    public ArrayList<GQuickAccess> quickAccessList;

    @SuppressWarnings("unused")
    public GInputListAction() {
    }

    public GInputListAction(String action, GAsyncEventExec asyncExec, ArrayList<GQuickAccess> quickAccessList) {
        this.action = action;
        this.asyncExec = asyncExec;
        this.quickAccessList = quickAccessList;
    }
}