package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncGroupObjectsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group objects";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupObjects();
    }
}
