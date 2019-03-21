package lsfusion.gwt.client.controller.remote.action.form;

public class GetRemoteChanges extends FormRequestIndexCountingAction<ServerResponseResult> {
    public boolean refresh;

    public GetRemoteChanges() {
        this(false);
    }

    public GetRemoteChanges(boolean refresh) {
        this.refresh = refresh;
    }
}
