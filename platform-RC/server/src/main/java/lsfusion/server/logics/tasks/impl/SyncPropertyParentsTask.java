package lsfusion.server.logics.tasks.impl;

public class SyncPropertyParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property parents";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyParents();
    }
}
