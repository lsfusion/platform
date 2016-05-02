package lsfusion.server.lib;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class PrepareLibraryClientAction implements ClientAction {
    String path;
    String filename;
    public byte[] bytes;

    public PrepareLibraryClientAction(String path, String filename, byte[] bytes) {
        this.path = path;
        this.filename = filename;
        this.bytes = bytes;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        File dllFile = new File(path + filename);
        FileUtils.writeByteArrayToFile(dllFile, bytes);
        return null;
    }
}