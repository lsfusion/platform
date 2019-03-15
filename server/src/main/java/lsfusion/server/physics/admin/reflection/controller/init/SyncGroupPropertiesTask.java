package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncGroupPropertiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group properties";
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupProperties();
    }
}
