package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncFormsTask extends SyncTask {

    @Override
    public String getCaption() {
        return "Synchronizing forms";
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    @Override
    public void runSync() {
        getReflectionManager().synchronizeForms();
    }
}
