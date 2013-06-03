package lsfusion.gwt.form.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

import java.util.Map;

public class GetForm implements Action<GetFormResult> {
    public String sid;
    public boolean isModal;
    public Map<String, String> initialObjects;

    public GetForm() {
    }

    public GetForm(String sid, boolean isModal, Map<String, String>initialObjects) {
        this.sid = sid;
        this.isModal = isModal;
        this.initialObjects = initialObjects;
    }
}
