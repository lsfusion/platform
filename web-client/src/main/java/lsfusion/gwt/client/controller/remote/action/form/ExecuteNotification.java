package lsfusion.gwt.client.controller.remote.action.form;

public class ExecuteNotification extends FormRequestCountingAction<ServerResponseResult> {
    public String notification;

    @SuppressWarnings("UnusedDeclaration")
    public ExecuteNotification() {
    }

    public ExecuteNotification(String notification) {
        this.notification = notification;
    }
}