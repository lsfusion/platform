package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public abstract class SyncTask extends ReflectionTask {

    protected abstract void runSync();

    protected boolean runInDebug() {
        return false;
    }

    public void run() {
        if ((!SystemProperties.isDebug || runInDebug()) && getReflectionManager().isSourceHashChanged()) {
            runSync();
        }
    }
}
