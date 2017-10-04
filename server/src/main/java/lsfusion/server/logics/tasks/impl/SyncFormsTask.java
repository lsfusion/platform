package lsfusion.server.logics.tasks.impl;

public class SyncFormsTask extends SyncTask {

    @Override
    public String getCaption() {
        return "Synchronizing forms";
    }

    @Override
    public void runSync() {
        getReflectionManager().synchronizeForms();
    }
}
