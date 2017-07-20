package lsfusion.server.logics.tasks.impl;

public class SyncPropertyDrawsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property draws";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyDraws();
    }
}
