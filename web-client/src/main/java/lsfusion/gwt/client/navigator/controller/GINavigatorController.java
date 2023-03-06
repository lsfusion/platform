package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.Map;

public interface GINavigatorController {

    GNavigatorElement getRoot();

    void update();

    void initMobileNavigatorView(GNavigatorWindow window, Widget widget);

    void openElement(GNavigatorAction element, NativeEvent event);

    void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows);

    void resetSelectedElements(GNavigatorElement newSelectedElement);
}
