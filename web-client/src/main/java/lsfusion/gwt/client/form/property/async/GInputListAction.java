package lsfusion.gwt.client.form.property.async;

import java.io.Serializable;
import java.util.ArrayList;

public class GInputListAction implements Serializable {
    public String action;
    public ArrayList<GQuickAccess> quickAccessList;

    @SuppressWarnings("unused")
    public GInputListAction() {
    }

    public GInputListAction(String action, ArrayList<GQuickAccess> quickAccessList) {
        this.action = action;
        this.quickAccessList = quickAccessList;
    }
}