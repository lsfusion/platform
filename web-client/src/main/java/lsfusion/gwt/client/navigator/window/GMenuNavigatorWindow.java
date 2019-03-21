package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GMenuNavigatorView;
import lsfusion.gwt.client.navigator.view.GNavigatorView;

public class GMenuNavigatorWindow extends GNavigatorWindow {
    public int showLevel;
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GMenuNavigatorView(this, navigatorController);
    }
}
