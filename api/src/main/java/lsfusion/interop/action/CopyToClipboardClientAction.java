package lsfusion.interop.action;

import java.io.IOException;

public class CopyToClipboardClientAction extends ExecuteClientAction {

    public String value;

    public CopyToClipboardClientAction(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CopyToClipboardClientAction";
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}