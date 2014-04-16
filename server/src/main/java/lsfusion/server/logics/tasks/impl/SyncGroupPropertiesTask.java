package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncGroupPropertiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group properties";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupProperties();
    }
}
