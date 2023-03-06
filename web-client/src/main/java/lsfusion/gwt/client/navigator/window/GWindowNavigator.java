package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.GPropertyNavigator;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public abstract class GWindowNavigator extends GPropertyNavigator {

    public String canonicalName;

    public GWindowNavigator() {
    }

    public GWindowNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public void update(GNavigatorController navigatorController, WindowsController windowsController, Object value) {
        GAbstractWindow window = windowsController.findWindowByCanonicalName(canonicalName);
        if(MainFrame.mobile && window == null)
            return;
        updateWindow(window, windowsController, value);
    }
    public abstract void updateWindow(GAbstractWindow window, WindowsController windowsController, Object value);
}
