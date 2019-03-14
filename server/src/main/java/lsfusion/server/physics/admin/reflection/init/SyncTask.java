package lsfusion.server.physics.admin.reflection.init;

import lsfusion.server.SystemProperties;
import org.apache.log4j.Logger;

public abstract class SyncTask extends ReflectionTask {

    protected abstract void runSync();

    protected boolean runInDebug() {
        return false;
    }

    @Override
    public boolean ignoreExceptions() {
        return true;
    }

    public void run(Logger logger) {
        if ((!SystemProperties.lightStart || runInDebug()) && getReflectionManager().isSourceHashChanged()) {
            runSync();
        }
    }
}
