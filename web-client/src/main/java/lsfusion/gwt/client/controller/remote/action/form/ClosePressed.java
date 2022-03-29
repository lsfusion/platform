package lsfusion.gwt.client.controller.remote.action.form;

public class ClosePressed extends FormRequestCountingAction<ServerResponseResult> {

    public boolean ok;

    public ClosePressed() {
    }

    public ClosePressed(boolean ok) {
        this.ok = ok;
    }
}
