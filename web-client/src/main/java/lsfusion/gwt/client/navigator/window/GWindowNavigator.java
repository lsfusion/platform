package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.GPropertyNavigator;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

public abstract class GWindowNavigator extends GPropertyNavigator {

    public String canonicalName;

    public GWindowNavigator() {
    }

    public GWindowNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public void update(GNavigatorController navigatorController, WindowsController windowsController, Object value) {
        updateWindow(windowsController.findWindowByCanonicalName(canonicalName), windowsController, value);

    }
    public abstract void updateWindow(GAbstractWindow window, WindowsController windowsController, Object value);
}
