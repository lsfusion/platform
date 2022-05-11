package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.navigator.NavigatorElement;

public class FinalizeNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement ne) {
        ne.finalizeAroundInit(getBL().LM);
    }

    public String getCaption() {
        return "Finalizing navigator elements";
    }
}
