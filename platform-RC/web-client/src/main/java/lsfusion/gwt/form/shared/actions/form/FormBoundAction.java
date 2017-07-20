package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.FormAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class FormBoundAction<R extends Result> extends RequestAction<R> implements FormAction {
    public String formSessionID;

    public FormBoundAction() {
    }

    @Override
    public String toString() {
        return this.getClass() + "[form#: " + formSessionID + "]";
    }
}
