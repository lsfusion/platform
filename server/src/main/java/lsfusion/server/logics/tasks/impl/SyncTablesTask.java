package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncTablesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing tables";
    }

    protected boolean runInDebug() {
        return true;
    }

    public void runSync() {
        getReflectionManager().synchronizeTables();
    }
}
