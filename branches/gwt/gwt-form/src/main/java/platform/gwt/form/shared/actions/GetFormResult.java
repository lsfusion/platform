package platform.gwt.form.shared.actions;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.view.GForm;

public class GetFormResult implements Result {
    public GForm form;

    public GetFormResult() {}

    public GetFormResult(GForm form) {
        this.form = form;
    }
}