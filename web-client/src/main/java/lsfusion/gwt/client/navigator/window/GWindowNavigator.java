package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.form.property.PValue;
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

    public void update(GNavigatorController navigatorController, WindowsController windowsController, PValue value) {
        GAbstractWindow window = windowsController.findWindowByCanonicalName(canonicalName);
        if(window == null) // MainFrame.mobile, can be null when window is "forbidden" with a security policy
            return;
        updateWindow(window, windowsController, value);
    }
    public abstract void updateWindow(GAbstractWindow window, WindowsController windowsController, PValue value);
}
