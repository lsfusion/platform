package lsfusion.interop.action;

import java.io.IOException;
import java.util.List;

public class InitJSClientAction extends ExecuteClientAction {

    public List<String> externalResources;

    public InitJSClientAction(List<String> externalResources) {
        this.externalResources = externalResources;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
