package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

public abstract class GElementNavigator extends GPropertyNavigator {
    public String canonicalName;

    public GElementNavigator() {
    }

    public GElementNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public void update(GNavigatorController navigatorController, WindowsController windowsController, PValue value) {
        updateElement(navigatorController.getElement(canonicalName), value);
    }
    public abstract void updateElement(GNavigatorElement element, PValue value);
}