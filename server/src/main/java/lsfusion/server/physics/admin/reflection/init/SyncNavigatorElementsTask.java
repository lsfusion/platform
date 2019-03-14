package lsfusion.server.physics.admin.reflection.init;

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
