package lsfusion.server.logics.tasks.impl;

import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.tasks.GroupNavigatorElementsTask;

public class FinalizeNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement form) {
        form.finalizeAroundInit();
    }

    public String getCaption() {
        return "Finalizing forms";
    }
}
