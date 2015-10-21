package lsfusion.interop.action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SaveFileClientAction extends ExecuteClientAction {
    public final byte[] file;
    public final String name;

    public SaveFileClientAction(byte[] file, String name) {
        this.file = file;
        this.name = name;
    }
    
    public Map<String, byte[]> getFileMap() {
        Map<String, byte[]> result = new HashMap<>();
        result.put(name, file);
        return result;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);    
    }
}
