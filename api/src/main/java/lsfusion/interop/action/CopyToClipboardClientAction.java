package lsfusion.interop.action;

import java.io.IOException;

public class CopyToClipboardClientAction implements ClientAction {

    public String value;

    public CopyToClipboardClientAction(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CopyToClipboardClientAction";
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}