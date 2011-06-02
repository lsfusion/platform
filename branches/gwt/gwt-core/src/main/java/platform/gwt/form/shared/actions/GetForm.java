package platform.gwt.form.shared.actions;

import net.customware.gwt.dispatch.shared.Action;

public class GetForm implements Action<GetFormResult> {
    public String sid;

    public GetForm() {
    }

    public GetForm(String sid) {
        this.sid = sid;
    }
}
