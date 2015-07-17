package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class ClientMessageResult implements Result {
    public boolean restart;
    public ClientMessageResult() {
    }

    public ClientMessageResult(boolean restart) {
        this.restart = restart;
    }
}
