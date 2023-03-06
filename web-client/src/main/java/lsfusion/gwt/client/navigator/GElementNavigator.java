package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

public abstract class GElementNavigator extends GPropertyNavigator {
    public String canonicalName;

    public GElementNavigator() {
    }

    public GElementNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public void update(GNavigatorController navigatorController, WindowsController windowsController, Object value) {
        updateElement(findNavigatorElementByCanonicalName(navigatorController.getRoot()), value);
    }
    public abstract void updateElement(GNavigatorElement element, Object value);

    protected GNavigatorElement findNavigatorElementByCanonicalName(GNavigatorElement root) {
        for(GNavigatorElement child : root.children) {
            if(child.canonicalName.equals(canonicalName)) {
                return child;
            } else {
                GNavigatorElement element = findNavigatorElementByCanonicalName(child);
                if(element != null) {
                    return element;
                }
            }
        }
        return null;
    }
}