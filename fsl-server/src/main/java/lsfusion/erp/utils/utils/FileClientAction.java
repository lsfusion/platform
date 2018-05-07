package lsfusion.erp.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;

public class FileClientAction implements ClientAction {
    private String source;
    private String destination;
    private int type;

    public FileClientAction(int type, String source) {
        this(type, source, null);
    }

    public FileClientAction(int type, String source, String destination) {
        this.type = type;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        switch (type) {
            case 0: { //FileExists
                return source != null && new File(source).exists();
            }
            case 1: {//DeleteFile
                File sourceFile = new File(source);
                if (!sourceFile.delete()) {
                    sourceFile.deleteOnExit();
                }
                return null;
            }
            case 2: {//MoveFile
                return new File(source).renameTo(new File(destination));
            }
            default:
                return null;
        }
    }

}