package platform.gwt.form2.client.navigator;

import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GAbstractWindow;

import java.util.Map;

public interface GINavigatorController {
    void update();

    void openElement(GNavigatorElement element);

    void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows);

    void setInitialSize(GAbstractWindow window, int width, int height);
}
