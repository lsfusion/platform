package lsfusion.server.physics.dev.integration.external.to.file.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

import java.io.IOException;

public class CopyFileClientAction implements ClientAction {
    private String source;
    private String destination;

    public CopyFileClientAction(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        String result = null;
        try {
            FileUtils.copyFile(source, destination);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to copy file from '%s' to '%s'", source, destination);
        }
        return result;
    }
}