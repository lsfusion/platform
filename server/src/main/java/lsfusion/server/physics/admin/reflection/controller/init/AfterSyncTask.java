package lsfusion.server.physics.admin.reflection.controller.init;

public class AfterSyncTask extends SyncTask {

    public String getCaption() {
        return "Executing afterSync action";
    }

    public void runSync() {
        getReflectionManager().executeAfterSync();
    }
}
