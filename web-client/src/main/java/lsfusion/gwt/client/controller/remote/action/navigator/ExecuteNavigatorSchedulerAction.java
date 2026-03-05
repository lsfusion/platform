package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.GNavigatorScheduler;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

public class ExecuteNavigatorSchedulerAction extends NavigatorRequestCountingAction<ServerResponseResult> {
    public GNavigatorScheduler navigatorScheduler;

    public ExecuteNavigatorSchedulerAction() {}

    public ExecuteNavigatorSchedulerAction(GNavigatorScheduler navigatorScheduler) {
        this.navigatorScheduler = navigatorScheduler;
    }
}
