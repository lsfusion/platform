package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.view.GNavigatorView;
import lsfusion.gwt.client.navigator.view.GPanelNavigatorView;

public class GPanelNavigatorWindow extends GNavigatorWindow {
    public int orientation;

    public boolean isVertical() {
        return orientation == 1;
    }

    public boolean isHorizontal() {
        return orientation == 0;
    }

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GPanelNavigatorView(this, navigatorController);
    }
}
