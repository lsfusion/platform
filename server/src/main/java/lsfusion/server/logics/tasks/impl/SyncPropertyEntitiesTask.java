package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.ReflectionTask;

public class SyncPropertyEntitiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property entities";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyEntities();
    }
}
