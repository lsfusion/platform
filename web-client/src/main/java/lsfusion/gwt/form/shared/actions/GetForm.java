package lsfusion.gwt.form.shared.actions;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;

import java.util.Map;

public class GetForm extends RequestAction<GetFormResult> implements NavigatorAction {
    public String sid;
    public String canonicalName;
    public boolean isModal;
    public Map<String, String> initialObjects;

    public GetForm() {
    }

    public GetForm(String canonicalName, String sid, boolean isModal, Map<String, String>initialObjects) {
        this.canonicalName = canonicalName;
        this.sid = sid;
        this.isModal = isModal;
        this.initialObjects = initialObjects;
    }
}
