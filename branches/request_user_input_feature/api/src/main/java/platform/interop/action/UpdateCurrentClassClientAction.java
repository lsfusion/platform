package platform.interop.action;

import java.io.IOException;

public class UpdateCurrentClassClientAction extends ExecuteClientAction {
    public final int currentClassId;

    public UpdateCurrentClassClientAction(int currentClassId) {
        this.currentClassId = currentClassId;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
