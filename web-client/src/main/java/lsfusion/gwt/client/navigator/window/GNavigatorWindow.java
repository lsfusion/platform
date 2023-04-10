package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.util.ArrayList;
import java.util.List;

public abstract class GNavigatorWindow extends GAbstractWindow {
    public List<GNavigatorElement> elements = new ArrayList<>();

    public boolean drawScrollBars;

    public abstract GNavigatorView createView(GINavigatorController navigatorController);
    public boolean isRoot() {
        return canonicalName.equals("System.root");
    }

    public boolean isSystem() {
        return canonicalName.equals("System.system");
    }

    public boolean isLogo() {
        return canonicalName.equals("System.logo");
    }
    
    public boolean isInRootNavBar() {
        return isLogo() || isRoot() || isSystem();
    }

    public boolean isToolbar() {
        return canonicalName.equals("System.toolbar");
    }

    public boolean isVertical() {
        return false;
    }

    public boolean forceDiv() {
        return elementClass != null && elementClass.contains(WindowsController.NAVBAR_TEXT_ON_HOVER);
    }
}
