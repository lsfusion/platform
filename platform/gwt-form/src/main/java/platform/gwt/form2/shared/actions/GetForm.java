package platform.gwt.form2.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

import java.util.Map;

public class GetForm implements Action<GetFormResult> {
    public String sid;
    public Map<String, String> initialObjects;

    public GetForm() {
    }

    public GetForm(String sid, Map<String, String>initialObjects) {
        this.sid = sid;
        this.initialObjects = initialObjects;
    }
}
