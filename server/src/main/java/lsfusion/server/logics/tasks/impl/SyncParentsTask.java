package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing parents";
    }

    public void runSync() {
        getReflectionManager().synchronizeParents();
    }
}
