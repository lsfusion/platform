package lsfusion.server.logics.tasks.impl;

public class SyncTablesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing tables";
    }

    protected boolean runInDebug() {
        return true;
    }

    public void runSync() {
        getReflectionManager().synchronizeTables();
    }
}
