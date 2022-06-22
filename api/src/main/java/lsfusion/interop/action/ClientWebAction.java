package lsfusion.interop.action;

import java.io.IOException;
import java.util.ArrayList;

public class ClientWebAction extends ExecuteClientAction {

    public Object resource;
    public final String resourceName;
    public final String originalResourceName;
    public ArrayList<byte[]> values;
    public ArrayList<byte[]> types;
    public byte[] returnType;
    public boolean isFile;
    public boolean syncType;

    public ClientWebAction(Object resource, String resourceName, String originalResourceName, ArrayList<byte[]> values, ArrayList<byte[]> types, byte[] returnType, boolean isFile, boolean syncType) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.originalResourceName = originalResourceName;
        this.values = values;
        this.types = types;
        this.returnType = returnType;
        this.isFile = isFile;
        this.syncType = syncType;
    }

    public boolean isFont() {
       return isFile && (resourceName.endsWith(".ttf") || resourceName.endsWith(".otf"));
    }

    public boolean isLibrary() {
        return isFile && (resourceName.endsWith(".dll") || resourceName.endsWith(".so"));
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
