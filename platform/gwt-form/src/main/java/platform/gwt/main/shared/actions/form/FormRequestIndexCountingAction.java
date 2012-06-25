package platform.gwt.main.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;

public class FormRequestIndexCountingAction<R extends Result> extends FormBoundAction<R> {
    public long requestIndex;

    public FormRequestIndexCountingAction() {
    }
}
