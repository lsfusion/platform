package lsfusion.utils.file.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.utils.file.FileUtils;

import java.io.IOException;

public class FileExistsClientAction implements ClientAction {
    private String source;

    public FileExistsClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return FileUtils.checkFileExists(source);
    }
}