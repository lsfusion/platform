package lsfusion.server.logics.tasks.impl;

public class SyncGroupObjectsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group objects";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupObjects();
    }
}
