package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class ReadClientAction implements ClientAction {
    String sourcePath;
    boolean isDynamicFormatFileClass;

    public ReadClientAction(String sourcePath, boolean isDynamicFormatFileClass) {
        this.sourcePath = sourcePath;
        this.isDynamicFormatFileClass = isDynamicFormatFileClass;

    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            return ReadUtils.readFile(sourcePath, isDynamicFormatFileClass);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}