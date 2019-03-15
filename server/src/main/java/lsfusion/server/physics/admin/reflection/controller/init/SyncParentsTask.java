package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing parents";
    }

    public void runSync() {
        getReflectionManager().synchronizeParents();
    }
}
