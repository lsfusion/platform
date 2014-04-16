package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncGroupObjectsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group objects";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupObjects();
    }
}
