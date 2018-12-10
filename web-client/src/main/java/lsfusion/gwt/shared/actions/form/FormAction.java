package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.result.RequestAction;
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
