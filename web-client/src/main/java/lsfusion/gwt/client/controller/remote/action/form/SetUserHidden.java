package lsfusion.gwt.client.controller.remote.action.form;

public class SetUserHidden extends FormRequestCountingAction<ServerResponseResult> {
    public int componentID;
    public boolean hidden;

    public SetUserHidden() {
    }

    public SetUserHidden(int componentID, boolean hidden) {
        this.componentID = componentID;
        this.hidden = hidden;
    }
}
