package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.window.NavigatorWindow;

public class CheckNavigatorElementsTask extends GroupNavigatorElementsTask {

    protected void runTask(NavigatorElement ne) {
        if (ne.isTopFolderWithoutWindow()) {
            throw new RuntimeException("Navigator folder " + ne + " in the top NAVIGATOR must specify a WINDOW for its children");
        }
        // only a React component both draws elements itself and can place a standard drawing; anywhere else the
        // platform draws every element anyway, so an LSF that reached such a window is a mistake, as it is on a form.
        // Checked here and not while the module is read: EXTEND WINDOW may give the window its component later
        if (ne.lsf && !isReactWindow(ne)) {
            throw new RuntimeException("LSF is set for navigator element " + ne + ", whose window is not drawn by a CUSTOM React component");
        }
    }

    private boolean isReactWindow(NavigatorElement ne) {
        NavigatorWindow window = ne.getDrawWindow();
        return window != null && window.isReact();
    }

    public String getCaption() {
        return "Checking navigator elements";
    }
}
