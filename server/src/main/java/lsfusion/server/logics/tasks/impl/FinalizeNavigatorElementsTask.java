package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.tasks.GroupNavigatorElementsTask;

public class FinalizeNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement ne) {
        ne.finalizeAroundInit();
    }

    public String getCaption() {
        return "Finalizing navigator elements";
    }
}
