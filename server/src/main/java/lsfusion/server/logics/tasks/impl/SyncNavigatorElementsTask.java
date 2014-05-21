package lsfusion.server.logics.tasks.impl;

public class SyncNavigatorElementsTask extends SyncTask {

    @Override
    public String getCaption() {
        return "Synchronizing navigator elements";
    }

    @Override
    public void runSync() {
        getReflectionManager().synchronizeNavigatorElements();
    }
}
