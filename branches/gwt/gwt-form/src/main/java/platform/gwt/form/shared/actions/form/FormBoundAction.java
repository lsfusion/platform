package platform.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class FormBoundAction<R extends Result> implements Action<R> {
    public String formSessionID;

    public FormBoundAction() {
    }

    public FormBoundAction(String formSessionID) {
        this.formSessionID = formSessionID;
    }
}
