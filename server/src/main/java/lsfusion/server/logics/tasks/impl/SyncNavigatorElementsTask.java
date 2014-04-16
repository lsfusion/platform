package lsfusion.server.logics.tasks.impl;

import lsfusion.server.SystemProperties;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.navigator.NavigatorAction;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.tasks.ReflectionTask;
import lsfusion.server.logics.tasks.Task;

import java.util.Set;

public class SyncNavigatorElementsTask extends SyncTask {

    @Override
    public String getCaption() {
        return "Synchronizing navigator elements";
    }

    @Override
    public void runSync() {
        getReflectionManager().synchronizeNavigatorElements();
    }
}
