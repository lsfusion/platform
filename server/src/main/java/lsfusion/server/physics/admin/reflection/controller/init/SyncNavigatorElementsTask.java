package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncNavigatorElementsTask extends SyncTask {

    @Override
    public String getCaption() {
        return "Synchronizing navigator elements";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    @Override
    public void runSync() {
        getReflectionManager().synchronizeNavigatorElements();
    }
}
