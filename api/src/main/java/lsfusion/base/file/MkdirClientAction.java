package lsfusion.base.file;

import lsfusion.base.FileUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class MkdirClientAction implements ClientAction {
    public final String source;

    public MkdirClientAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        String result = null;
        try {
            FileUtils.mkdir(source);
        } catch (Exception e) {
            result = e.getMessage() != null ? e.getMessage() : String.format("Failed to create directory '%s'", source);
        }
        return result;
    }
}