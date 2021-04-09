package lsfusion.gwt.client.controller.remote.action.form;

public class GetRemoteChanges extends FormRequestCountingAction<ServerResponseResult> {
    public boolean refresh;
    public boolean forceLocalEvents;

    public GetRemoteChanges() {
    }

    public GetRemoteChanges(boolean forceLocalEvents) {
        this(false, forceLocalEvents);
    }

    public GetRemoteChanges(boolean refresh, boolean forceLocalEvents) {
        this.refresh = refresh;
        this.forceLocalEvents = forceLocalEvents;
    }
}
