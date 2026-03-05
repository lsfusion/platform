package lsfusion.base.file;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class DeleteFileClientAction implements ClientAction {
    public final String source;

    public DeleteFileClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {

        String result = null;
        try {
            FileUtils.delete(source);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to delete '%s'", source);
        }
        return result;
    }
}