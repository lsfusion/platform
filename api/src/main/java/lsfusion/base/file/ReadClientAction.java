package lsfusion.base.file;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

public class ReadClientAction implements ClientAction {
    public final String sourcePath;
    public final boolean isDynamicFormatFileClass;
    public final boolean isBlockingFileRead;
    public final boolean isDialog;

    public ReadClientAction(String sourcePath, boolean isDynamicFormatFileClass, boolean isBlockingFileRead, boolean isDialog) {
        this.sourcePath = sourcePath;
        this.isDynamicFormatFileClass = isDynamicFormatFileClass;
        this.isBlockingFileRead = isBlockingFileRead;
        this.isDialog = isDialog;

    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        try {
            return ReadUtils.readFile(sourcePath, isBlockingFileRead, isDialog, null);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}