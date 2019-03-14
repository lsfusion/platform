package lsfusion.server.physics.admin.reflection.init;

public class SyncGroupObjectsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group objects";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupObjects();
    }
}
