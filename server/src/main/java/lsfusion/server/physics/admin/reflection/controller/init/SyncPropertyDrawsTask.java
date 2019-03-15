package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncPropertyDrawsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property draws";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyDraws();
    }
}
