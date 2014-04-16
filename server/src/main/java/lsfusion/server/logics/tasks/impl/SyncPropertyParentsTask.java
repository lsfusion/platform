package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncPropertyParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property parents";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyParents();
    }
}
