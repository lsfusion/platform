package lsfusion.server.physics.dev.integration.external.to.file;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

public abstract class FileAlterationListener extends FileAlterationListenerAdaptor {
    private final String src;

    public FileAlterationListener(String src) {
        this.src = src;
    }

    public String getSrc() {
        return src;
    }
}
