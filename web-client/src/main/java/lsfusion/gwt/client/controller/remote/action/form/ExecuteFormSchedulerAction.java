package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.GFormScheduler;

public class ExecuteFormSchedulerAction extends FormRequestCountingAction<ServerResponseResult> {
    public GFormScheduler formScheduler;

    public ExecuteFormSchedulerAction() {
    }

    public ExecuteFormSchedulerAction(GFormScheduler formScheduler) {
        this.formScheduler = formScheduler;
    }
}