package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.form.shared.view.GDefaultFormsType;
import net.customware.gwt.dispatch.shared.Result;

public class ShowDefaultFormsResult implements Result {
    public GDefaultFormsType defaultFormsType;

    @SuppressWarnings("UnusedDeclaration")
    public ShowDefaultFormsResult() {
    }

    public ShowDefaultFormsResult(GDefaultFormsType defaultFormsType) {
        this.defaultFormsType = defaultFormsType;
    }
}
