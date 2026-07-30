package lsfusion.gwt.client.navigator.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.Map;

public interface GINavigatorController {

    GNavigatorElement getRoot();

    // the element with this canonical name anywhere in the navigator, or null - a custom view uses it to tell a name
    // that is simply wrong from one this window merely does not draw right now
    GNavigatorElement getElement(String canonicalName);

    void update();

    void initMobileNavigatorView(GNavigatorWindow window, Widget widget);

    void activate(GNavigatorElement element, NativeEvent event);

    // the JS controller a custom view hands to its component; owned by the dispatcher, which is built after this
    JavaScriptObject getController();

    void openElement(GNavigatorAction element, NativeEvent event);

    void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows);
}
