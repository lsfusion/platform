package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.VoidResult;

public class RefreshUPHiddenPropsAction extends FormRequestIndexCountingAction<VoidResult> {
    public String[] propSids;

    public RefreshUPHiddenPropsAction() {
        this(new String[0]);
    }

    public RefreshUPHiddenPropsAction(String[] propSids) {
        this.propSids = propSids;
    }
}
