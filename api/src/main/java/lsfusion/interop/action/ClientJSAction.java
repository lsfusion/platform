package lsfusion.interop.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientJSAction extends ExecuteClientAction {

    public List<String> externalResources;
    public ArrayList<byte[]> values;
    public ArrayList<byte[]> types;
    public boolean isFile;

    public ClientJSAction(List<String> externalResources, ArrayList<byte[]> values, ArrayList<byte[]> types, boolean isFile) {
        this.externalResources = externalResources;
        this.values = values;
        this.types = types;
        this.isFile = isFile;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
