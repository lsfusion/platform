package lsfusion.server.logics.property.actions.file;

import com.google.common.base.Throwables;
import lsfusion.base.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;

public class WriteClientAction extends ExecuteClientAction {
    RawFileData file;
    String path;
    String extension;
    boolean append;

    public WriteClientAction(RawFileData file, String path, String extension, boolean append) {
        this.file = file;
        this.path = path;
        this.extension = extension;
        this.append = append;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        try {
            WriteUtils.write(file, path, extension, append);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}