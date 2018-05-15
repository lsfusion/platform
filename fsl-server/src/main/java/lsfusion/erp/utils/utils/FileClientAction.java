package lsfusion.erp.utils.utils;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

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
                return sourceFile.exists() && sourceFile.delete();
            }
            case 2: {//MoveFile
                return new File(source).renameTo(new File(destination));
            }
            case 3: { //ListFiles
                TreeMap<String, Boolean> result = new TreeMap<>();
                if (source != null) {
                    File[] filesList = new File(source).listFiles();
                    if (filesList != null) {
                        for (File file : filesList) {
                            result.put(file.getName(), file.isDirectory());
                        }
                    }
                }
                return result;
            }
            default:
                return null;
        }
    }

}