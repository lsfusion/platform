package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class ClientMessageResult implements Result {
    public Integer clientMessage;
    public ClientMessageResult() {
    }

    public ClientMessageResult(Integer clientMessage) {
        this.clientMessage = clientMessage;
    }
}
