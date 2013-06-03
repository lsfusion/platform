package lsfusion.interop.action;

import lsfusion.interop.form.RemoteDialogInterface;

import java.io.IOException;

public class DialogClientAction extends ExecuteClientAction {
    public RemoteDialogInterface dialog;

    public DialogClientAction(RemoteDialogInterface dialog) {
        this.dialog = dialog;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
