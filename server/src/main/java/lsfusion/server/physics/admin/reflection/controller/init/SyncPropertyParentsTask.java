package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncPropertyParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property parents";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyParents();
    }
}
