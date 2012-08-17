package platform.gwt.form2.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.GForm;

public class GetFormResult implements Result {
    public GForm form;

    @SuppressWarnings("UnusedDeclaration")
    public GetFormResult() {}

    public GetFormResult(GForm form) {
        this.form = form;
    }
}