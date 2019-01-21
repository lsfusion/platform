package lsfusion.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class DeleteFileClientAction implements ClientAction {
    private String source;

    public DeleteFileClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        String result = null;
        try {
            FileUtils.delete(source);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to delete '%s'", source);
        }
        return result;
    }
}