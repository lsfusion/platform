package lsfusion.server.lib;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;


public class LoadDownloadedLibraryClientAction implements ClientAction {
    String path;
    String filename;

    public LoadDownloadedLibraryClientAction(String path, String filename) {
        this.path = path;
        this.filename = filename;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        File dll = new File(path + filename);
        if(dll.exists())
            setJNALibraryPath(dll.getParentFile().getAbsolutePath());
        return null;
    }

    protected String setJNALibraryPath(String path) throws IOException {
        String javaLibraryPath = System.getProperty("jna.library.path");
        if (javaLibraryPath == null) {
            System.setProperty("jna.library.path", path);
        } else if (!javaLibraryPath.contains(path)) {
            System.setProperty("jna.library.path", path + ";" + javaLibraryPath);
        }
        return path;
    }
}