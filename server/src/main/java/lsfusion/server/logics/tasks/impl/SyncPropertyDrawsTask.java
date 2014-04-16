package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncPropertyDrawsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property draws";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyDraws();
    }
}
