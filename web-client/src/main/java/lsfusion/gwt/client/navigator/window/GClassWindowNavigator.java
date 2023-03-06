package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class GClassWindowNavigator extends GWindowNavigator {

    public GClassWindowNavigator() {
    }

    public GClassWindowNavigator(String canonicalName) {
        super(canonicalName);
    }

    @Override
    public void updateWindow(GAbstractWindow window, WindowsController windowsController, Object value) {
        window.elementClass = (String) value;

        windowsController.updateElementClass(window);
    }
}
