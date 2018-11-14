package lsfusion.interop.action;

import lsfusion.base.RawFileData;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveFileClientAction extends ExecuteClientAction {
    public final RawFileData file;
    public final String path;
    public final boolean noDialog;
    public final boolean append;

    public SaveFileClientAction(RawFileData file, String path, boolean noDialog, boolean append) {
        this.file = file;
        this.path = path;
        this.noDialog = noDialog;
        this.append = append;
    }
    
    public Map<String, RawFileData> getFileMap() {
        Map<String, RawFileData> result = new HashMap<>();
        result.put(path, file);
        return result;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);    
    }
}
