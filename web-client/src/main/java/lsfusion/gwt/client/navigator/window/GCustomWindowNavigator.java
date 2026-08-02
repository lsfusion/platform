package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

// CUSTOM <property>: the window's HTML template changed, so the window is drawn again from the new markup
public class GCustomWindowNavigator extends GWindowNavigator {

    public GCustomWindowNavigator() {
    }

    public GCustomWindowNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateWindow(GAbstractWindow window, GNavigatorController navigatorController, WindowsController windowsController, PValue value) {
        String custom = PValue.getStringValue(value);
        if (custom == null) // "nothing computed yet", not "no template": the window keeps the markup it has - the
            return;         // literal that was written beside the property, or the placeholder standing in for one

        window.custom = custom;

        navigatorController.update(); // the view re-renders the template, keeping the buttons it already built
    }
}
