package lsfusion.interop.action;

import lsfusion.base.RawFileData;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ExportFileClientAction extends ExecuteClientAction {

    // в качестве ключей - имена файлов, не пути к ним
    public Map<String, RawFileData> files;

    public ExportFileClientAction(String fileName, RawFileData file) {
        files = new HashMap<>();
        files.put(fileName, file);
    }

    public ExportFileClientAction(Map<String, RawFileData> files) {
        this.files = files;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
