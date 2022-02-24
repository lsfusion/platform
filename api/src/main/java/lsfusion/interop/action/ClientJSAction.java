package lsfusion.interop.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientJSAction extends ExecuteClientAction {

    public List<String> externalResources;
    public ArrayList<byte[]> keys;

    public ClientJSAction(List<String> externalResources, ArrayList<byte[]> keys) {
        this.externalResources = externalResources;
        this.keys = keys;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
