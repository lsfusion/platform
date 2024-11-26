package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncPropertyDrawsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing property draws";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public void runSync() {
        getReflectionManager().synchronizePropertyDraws();
    }
}
