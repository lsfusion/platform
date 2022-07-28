package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.GFormEvent;

public class ExecuteFormEventAction extends FormRequestCountingAction<ServerResponseResult> {
    public GFormEvent formEvent;

    public ExecuteFormEventAction() {
    }

    public ExecuteFormEventAction(GFormEvent formEvent) {
        this.formEvent = formEvent;
    }
}