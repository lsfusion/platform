package lsfusion.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public class FormBoundAction<R extends Result> implements Action<R> {
    public String formSessionID;

    public FormBoundAction() {
    }

    @Override
    public String toString() {
        return this.getClass() + "[form#: " + formSessionID + "]";
    }
}
