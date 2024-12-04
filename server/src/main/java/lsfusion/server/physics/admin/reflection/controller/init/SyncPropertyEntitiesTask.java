package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncPropertyEntitiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property entities";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyEntities();
    }
}
