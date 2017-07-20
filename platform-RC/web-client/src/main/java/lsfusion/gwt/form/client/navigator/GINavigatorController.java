package lsfusion.gwt.form.client.navigator;

import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.form.shared.view.GNavigatorElement;
import lsfusion.gwt.form.shared.view.window.GAbstractWindow;

import java.util.Map;

public interface GINavigatorController {
    void update();

    void openElement(GNavigatorElement element, NativeEvent event);

    void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows);

    void setInitialSize(GAbstractWindow window, int width, int height);
}
