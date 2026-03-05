package lsfusion.base.file;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class ListFilesClientAction implements ClientAction {
    public final String source;
    public final boolean recursive;

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