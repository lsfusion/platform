package lsfusion.server.physics.admin.reflection.controller.init;

public class SyncNavigatorElementParentsTask extends SyncTask {

    public String getCaption() {
        return "Synchronizing navigator element parents";
    }

    @Override
    public boolean isEndLoggable() {
        return true;
    }

    public void runSync() {
        getReflectionManager().synchronizeNavigatorElementParents();
    }
}
