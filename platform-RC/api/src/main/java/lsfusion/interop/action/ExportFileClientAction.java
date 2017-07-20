package lsfusion.interop.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ExportFileClientAction extends ExecuteClientAction {

    // в качестве ключей - имена файлов, не пути к ним
    public Map<String, byte[]> files;

    public ExportFileClientAction(String fileName, String fileText, String charsetName) {
        try {
            files = new HashMap<>();
            if (charsetName != null) {
                files.put(fileName, fileText.getBytes(charsetName));
            } else {
                files.put(fileName, fileText.getBytes());
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public ExportFileClientAction(String fileName, byte[] file) {
        files = new HashMap<>();
        files.put(fileName, file);
    }

    public ExportFileClientAction(Map<String, byte[]> files) {
        this.files = files;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
