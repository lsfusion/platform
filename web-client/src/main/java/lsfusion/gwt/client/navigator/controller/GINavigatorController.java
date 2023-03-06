package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;

import java.util.Map;

public interface GINavigatorController {

    GNavigatorElement getRoot();

    void update();

    void openElement(GNavigatorAction element, NativeEvent event);

    void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows);

    void autoSizeWindows();

    void resetSelectedElements(GNavigatorElement newSelectedElement);
}
