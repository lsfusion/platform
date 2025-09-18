package lsfusion.base.file;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.File;
import java.io.IOException;

public class WriteClientAction extends ExecuteClientAction {
    public final NamedFileData file;
    public final String path;
    public final boolean append;
    public final boolean isDialog;

    public WriteClientAction(File file, String path) throws IOException {
        this(new NamedFileData(new RawFileData(file)), path, false, true);
   }

    public WriteClientAction(NamedFileData file, String path, boolean append, boolean isDialog) {
        this.file = file;
        this.path = path;
        this.append = append;
        this.isDialog = isDialog;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        try {
            String filePath = path;
            if (isDialog)
                filePath = FileDialogUtils.showSaveFileDialog(WriteUtils.appendExtension(path, file), null);
            if (filePath != null) {
                WriteUtils.write(file, filePath, true, append);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}