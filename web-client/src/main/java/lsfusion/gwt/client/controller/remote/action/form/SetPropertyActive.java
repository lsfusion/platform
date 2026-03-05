package lsfusion.gwt.client.controller.remote.action.form;

public class SetPropertyActive extends FormRequestCountingAction<ServerResponseResult> {
    public int propertyId;
    public boolean focused;

    public SetPropertyActive() {
    }

    public SetPropertyActive(int propertyId, boolean focused) {
        this.propertyId = propertyId;
        this.focused = focused;
    }
}
