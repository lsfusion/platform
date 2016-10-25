package lsfusion.server.lib;

import lsfusion.base.SystemUtils;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;


public class LoadDownloadedLibraryClientAction implements ClientAction {
    String path;

    public LoadDownloadedLibraryClientAction(String path) {
        this.path = path;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        File dll = SystemUtils.getUserFile(path);
        if(dll.exists())
            setJNALibraryPath(dll.getParentFile().getAbsolutePath());
        else
            ClientActionLogger.logger.info("dll not found: " + dll.getAbsolutePath());
        return null;
    }

    protected String setJNALibraryPath(String path) throws IOException {
        String javaLibraryPath = System.getProperty("jna.library.path");
        ClientActionLogger.logger.info("old jna.library.path = " + javaLibraryPath);
        if (javaLibraryPath == null) {
            System.setProperty("jna.library.path", path);
        } else if (!javaLibraryPath.contains(path)) {
            System.setProperty("jna.library.path", path + ";" + javaLibraryPath);
        }
        ClientActionLogger.logger.info("new jna.library.path = " + System.getProperty("jna.library.path"));
        return path;
    }
}