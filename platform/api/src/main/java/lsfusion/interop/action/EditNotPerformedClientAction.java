package lsfusion.interop.action;

import java.io.IOException;

public class EditNotPerformedClientAction extends ExecuteClientAction {
    public static EditNotPerformedClientAction instance = new EditNotPerformedClientAction();

    private EditNotPerformedClientAction() {}

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
