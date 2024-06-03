package lsfusion.interop.action;

import java.io.IOException;

public class UnloadResourceClientAction extends ExecuteClientAction {
    public String resourceName;

    public UnloadResourceClientAction(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}