package lsfusion.gwt.form.shared.actions.form;

public class GetRemoteChanges extends FormRequestIndexCountingAction<ServerResponseResult> {
    public boolean refresh;

    public GetRemoteChanges() {
        this(false);
    }

    public GetRemoteChanges(boolean refresh) {
        this.refresh = refresh;
    }
}
