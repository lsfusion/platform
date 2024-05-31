package lsfusion.interop.action;

import java.io.IOException;

public class UnloadResourceClientAction extends ExecuteClientAction {
    public String resource;

    public UnloadResourceClientAction(String resource) {
        this.resource = resource;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}