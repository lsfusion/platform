package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncTablesTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing tables";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    protected boolean runInDebug() {
        return true;
    }

    public void runSync() {
        getReflectionManager().synchronizeTables();
    }
}
