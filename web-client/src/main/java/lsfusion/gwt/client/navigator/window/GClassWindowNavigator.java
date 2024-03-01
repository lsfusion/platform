package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

public class GClassWindowNavigator extends GWindowNavigator {

    public GClassWindowNavigator() {
    }

    public GClassWindowNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateWindow(GAbstractWindow window, WindowsController windowsController, PValue value) {
        window.elementClass = PValue.getClassStringValue(value);

        windowsController.updateElementClass(window);
    }
}
