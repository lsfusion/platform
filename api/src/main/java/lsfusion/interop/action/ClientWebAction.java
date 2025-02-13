package lsfusion.interop.action;

import lsfusion.base.BaseUtils;

import java.io.IOException;
import java.util.ArrayList;

public class ClientWebAction extends ExecuteClientAction {

    public Object resource;
    public final String resourceName;
    public final String originalResourceName;
    public boolean isFile;

    public ArrayList<byte[]> values;
    public ArrayList<byte[]> types;
    public byte[] returnType;
    public boolean syncType;
    public boolean remove;

    public ClientWebAction(Object resource, String resourceName, String originalResourceName, boolean isFile, ArrayList<byte[]> values,
                           ArrayList<byte[]> types, byte[] returnType, boolean syncType, boolean remove) {
        this.resource = resource;
        this.resourceName = resourceName;
        this.originalResourceName = originalResourceName;
        this.values = values;
        this.types = types;
        this.returnType = returnType;
        this.isFile = isFile;
        this.syncType = syncType;
        this.remove = remove;
    }

    public boolean isFont() {
        return isFile && (BaseUtils.endsWithIgnoreCase(resourceName, ".ttf") || BaseUtils.endsWithIgnoreCase(resourceName, ".otf"));
    }

    public boolean isLibrary() {
        return isFile && (BaseUtils.endsWithIgnoreCase(resourceName, ".dll") || BaseUtils.endsWithIgnoreCase(resourceName, ".so"));
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
