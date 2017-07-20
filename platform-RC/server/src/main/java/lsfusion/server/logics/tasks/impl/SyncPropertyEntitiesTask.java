package lsfusion.server.logics.tasks.impl;

public class SyncPropertyEntitiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property entities";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyEntities();
    }
}
