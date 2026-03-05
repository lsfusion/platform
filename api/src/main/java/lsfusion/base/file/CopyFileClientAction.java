package lsfusion.base.file;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class CopyFileClientAction implements ClientAction {
    public final String source;
    public final String destination;

    public CopyFileClientAction(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        String result = null;
        try {
            FileUtils.copyFile(source, destination);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to copy file from '%s' to '%s'", source, destination);
        }
        return result;
    }
}