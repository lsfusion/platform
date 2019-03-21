package lsfusion.gwt.shared.view.window;

import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.GNavigatorView;
import lsfusion.gwt.client.navigator.GTreeNavigatorView;

public class GTreeNavigatorWindow extends GNavigatorWindow {
    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GTreeNavigatorView(this, navigatorController);
    }
}
