package lsfusion.interop.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveFileClientAction extends ExecuteClientAction {
    public final byte[] file;
    public final String path;
    public final boolean relative;
    public final boolean noDialog;
    public final boolean append;

    public SaveFileClientAction(byte[] file, String path, boolean relative, boolean noDialog, boolean append) {
        this.file = file;
        this.path = path;
        this.relative = relative;
        this.noDialog = noDialog;
        this.append = append;
    }
    
    public Map<String, byte[]> getFileMap() {
        Map<String, byte[]> result = new HashMap<>();
        result.put(noDialog && relative ? (System.getProperty("user.home") + "/Downloads/" + path) : path, file);
        return result;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);    
    }
}
