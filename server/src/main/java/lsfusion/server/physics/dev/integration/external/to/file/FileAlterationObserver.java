package lsfusion.server.physics.dev.integration.external.to.file;

public class FileAlterationObserver extends org.apache.commons.io.monitor.FileAlterationObserver {
    public FileAlterationObserver(FileAlterationListener listener) {
        super(listener.getSrc());
        try {
            addListener(listener);
            initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
