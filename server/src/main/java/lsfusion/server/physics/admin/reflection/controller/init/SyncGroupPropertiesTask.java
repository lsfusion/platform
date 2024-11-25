package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncGroupPropertiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing group properties";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public void runSync() {
        getReflectionManager().synchronizeGroupProperties();
    }
}
