package lsfusion.interop.action;

import java.io.IOException;
import java.util.ArrayList;

public class ClientJSAction extends ExecuteClientAction {

    public String resource;
    public final String resourceName;
    public ArrayList<byte[]> values;
    public ArrayList<byte[]> types;
    public boolean isFile;

    public ClientJSAction(String resource, String resourceName, ArrayList<byte[]> values, ArrayList<byte[]> types, boolean isFile) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.values = values;
        this.types = types;
        this.isFile = isFile;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
