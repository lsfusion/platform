package lsfusion.interop.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveFileClientAction extends ExecuteClientAction {
    public final byte[] file;
    public final String extension;

    public SaveFileClientAction(byte[] file, String extension) {
        this.file = file;
        this.extension = extension;
    }
    
    public Map<String, byte[]> getFileMap() {
        Map<String, byte[]> result = new HashMap<>();
        result.put("new file." + extension, file);
        return result;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);    
    }
}
