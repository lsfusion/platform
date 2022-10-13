package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.GFormEvent;
import lsfusion.gwt.client.form.property.async.GPushAsyncResult;

public class ExecuteFormEventAction extends FormRequestCountingAction<ServerResponseResult> {
    public GFormEvent formEvent;
    public GPushAsyncResult pushAsyncResult;

    public ExecuteFormEventAction() {
    }

    public ExecuteFormEventAction(GFormEvent formEvent) {
        this.formEvent = formEvent;
    }
}