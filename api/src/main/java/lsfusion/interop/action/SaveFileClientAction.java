package lsfusion.interop.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveFileClientAction extends ExecuteClientAction {
    public final byte[] file;
    public final String path;
    public final boolean noDialog;

    public SaveFileClientAction(byte[] file, String path, boolean noDialog) {
        this.file = file;
        this.path = path;
        this.noDialog = noDialog;
    }
    
    public Map<String, byte[]> getFileMap() {
        Map<String, byte[]> result = new HashMap<>();
        result.put(path, file);
        return result;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);    
    }
}
