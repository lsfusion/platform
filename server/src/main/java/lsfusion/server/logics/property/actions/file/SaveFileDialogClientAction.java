package lsfusion.server.logics.property.actions.file;

import com.google.common.base.Throwables;
import lsfusion.base.FileDialogUtils;
import lsfusion.base.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;

public class SaveFileDialogClientAction extends ExecuteClientAction {
    RawFileData file;
    String path;

    public SaveFileDialogClientAction(RawFileData file, String path) {
        this.file = file;
        this.path = path;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        try {
            FileDialogUtils.showSaveFileDialog(path, file);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}