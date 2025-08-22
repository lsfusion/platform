package lsfusion.base.file;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class FileExistsClientAction implements ClientAction {
    public final String source;

    public FileExistsClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return FileUtils.checkFileExists(source);
    }
}