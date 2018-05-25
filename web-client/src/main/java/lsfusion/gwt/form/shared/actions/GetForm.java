package lsfusion.gwt.form.shared.actions;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;

import java.util.Map;

public class GetForm extends RequestAction<GetFormResult> implements NavigatorAction {
    public String sid;
    public String canonicalName;
    public boolean isModal;
    public String tabSID;

    public GetForm() {
    }
    
    public GetForm(String canonicalName, String sid, boolean isModal, String tabSID) {
        this.canonicalName = canonicalName;
        this.sid = sid;
        this.isModal = isModal;
        this.tabSID = tabSID;
    }
}
