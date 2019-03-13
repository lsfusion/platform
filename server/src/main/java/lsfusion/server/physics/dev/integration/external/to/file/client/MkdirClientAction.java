package lsfusion.server.physics.dev.integration.external.to.file.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

import java.io.IOException;

public class MkdirClientAction implements ClientAction {
    private String source;

    public MkdirClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        String result = null;
        try {
            FileUtils.mkdir(source);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to create directory '%s'", source);
        }
        return result;
    }
}