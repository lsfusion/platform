package lsfusion.gwt.form.shared.actions.form;

public class ExecuteNotification extends FormRequestIndexCountingAction<ServerResponseResult> {
    public Integer idNotification;

    @SuppressWarnings("UnusedDeclaration")
    public ExecuteNotification() {
    }

    public ExecuteNotification(Integer idNotification) {
        this.idNotification = idNotification;
    }
}