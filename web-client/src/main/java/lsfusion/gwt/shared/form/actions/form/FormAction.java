package lsfusion.gwt.shared.form.actions.form;

import lsfusion.gwt.shared.base.actions.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class FormAction<R extends Result> extends RequestAction<R> {
    public String formSessionID;

    public FormAction() {
    }

    @Override
    public String toString() {
        return this.getClass() + "[form#: " + formSessionID + "]";
    }
}
