package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;
import org.apache.log4j.Logger;

public abstract class SyncTask extends ReflectionTask {

    protected abstract void runSync();

    protected boolean runInDebug() {
        return false;
    }

    public void run(Logger logger) {
        if ((!SystemProperties.isDebug || runInDebug()) && getReflectionManager().isSourceHashChanged()) {
            try {
                runSync();
            } catch (Exception e) {
                logger.error("SyncTask error: ", e);
            }
        }
    }
}
