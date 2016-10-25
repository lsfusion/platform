package lsfusion.server.lib;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class DownloadFileClientAction implements ClientAction {
    String path;
    public byte[] bytes;

    public DownloadFileClientAction(String path, byte[] bytes) {
        this.path = path;
        this.bytes = bytes;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            SystemUtils.writeUserFile(path, bytes);
        } catch (Exception e) {
            ClientActionLogger.logger.error("DownloadFile Error", e);
        }
        return null;
    }
}