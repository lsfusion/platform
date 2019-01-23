package lsfusion.base.file;

import com.google.common.base.Throwables;
import lsfusion.base.FileDialogUtils;
import lsfusion.base.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.IOException;

public class WriteClientAction extends ExecuteClientAction {
    private final RawFileData file;
    private final String path;
    private final String extension;
    private final boolean append;
    private final boolean isDialog;

    public WriteClientAction(RawFileData file, String path, String extension, boolean append, boolean isDialog) {
        this.file = file;
        this.path = path;
        this.extension = extension;
        this.append = append;
        this.isDialog = isDialog;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        try {
            String filePath = path;
            String fileExtension = extension;
            if (isDialog) {
                filePath = FileDialogUtils.showSaveFileDialog(WriteUtils.appendExtension(path, extension), file);
                fileExtension = null;
            }
            if (filePath != null) {
                WriteUtils.write(file, filePath, fileExtension, append);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}