package lsfusion.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;

public class FormRequestIndexCountingAction<R extends Result> extends FormBoundAction<R> {
    public int requestIndex;

    public FormRequestIndexCountingAction() {
    }

    @Override
    public String toString() {
        return super.toString() + " [request#: " + requestIndex + "]";
    }
}
