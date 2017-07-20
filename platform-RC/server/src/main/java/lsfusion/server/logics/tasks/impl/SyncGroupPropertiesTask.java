package lsfusion.server.logics.tasks.impl;

public class SyncGroupPropertiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group properties";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupProperties();
    }
}
