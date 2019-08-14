package lsfusion.server.physics.dev.integration.external.to.file.client;

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

    public Object dispatch(ClientActionDispatcher dispatcher) {
        File library = SystemUtils.getUserFile(path);
        if(library.exists()) {
            setLibraryPath(library.getParentFile().getAbsolutePath(), "jna.library.path");
            setLibraryPath(library.getParentFile().getAbsolutePath(), "java.library.path");
        }
        else
            ClientActionLogger.logger.info("library not found: " + library.getAbsolutePath());
        return null;
    }

    protected void setLibraryPath(String path, String property) {
        String libraryPath = System.getProperty(property);
        ClientActionLogger.logger.info("old " + property + " = " + libraryPath);
        if (libraryPath == null) {
            System.setProperty(property, path);
        } else if (!libraryPath.contains(path)) {
            System.setProperty(property, path + ";" + libraryPath);
        }
        ClientActionLogger.logger.info("new " + property + " = " + System.getProperty(property));
    }
}