package lsfusion.gwt.client.controller.remote.action.form;

public class ExecuteFormSchedulerAction extends FormRequestCountingAction<ServerResponseResult> {
    public int index;

    public ExecuteFormSchedulerAction() {
    }

    public ExecuteFormSchedulerAction(int index) {
        this.index = index;
    }
}