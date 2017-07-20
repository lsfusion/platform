package lsfusion.server.logics.tasks.impl;

public class SyncParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing parents";
    }

    public void runSync() {
        getReflectionManager().synchronizeParents();
    }
}
