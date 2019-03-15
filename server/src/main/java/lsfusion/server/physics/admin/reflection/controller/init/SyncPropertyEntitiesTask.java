package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncPropertyEntitiesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property entities";
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyEntities();
    }
}
