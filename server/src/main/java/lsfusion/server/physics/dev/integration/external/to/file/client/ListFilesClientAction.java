package lsfusion.server.physics.dev.integration.external.to.file.client;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

public class ListFilesClientAction implements ClientAction {
    private String source;
    private boolean recursive;

    public ListFilesClientAction(String source, boolean recursive) {
        this.source = source;
        this.recursive = recursive;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        Object result;
        try {
            result = FileUtils.listFiles(source, recursive);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to listFiles '%s'", source);
        }
        return result;
    }
}