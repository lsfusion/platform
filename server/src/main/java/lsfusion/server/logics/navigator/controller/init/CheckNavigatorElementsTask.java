package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.navigator.NavigatorElement;

public class CheckNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement ne) {
        if (ne.isTopFolderWithoutWindow()) {
            throw new RuntimeException("Navigator folder " + ne + " in the top NAVIGATOR must specify a WINDOW for its children");
        }
    }

    public String getCaption() {
        return "Checking navigator elements";
    }
}
