package lsfusion.base.file;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class ReadClientAction implements ClientAction {
    String sourcePath;
    boolean isDynamicFormatFileClass;
    boolean isBlockingFileRead;
    boolean isDialog;

    public ReadClientAction(String sourcePath, boolean isDynamicFormatFileClass, boolean isBlockingFileRead, boolean isDialog) {
        this.sourcePath = sourcePath;
        this.isDynamicFormatFileClass = isDynamicFormatFileClass;
        this.isBlockingFileRead = isBlockingFileRead;
        this.isDialog = isDialog;

    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        try {
            return ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, isDialog, null);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}