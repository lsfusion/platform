package platform.gwt.form2.shared.view.window;

import platform.gwt.form2.client.navigator.GINavigatorController;
import platform.gwt.form2.client.navigator.GNavigatorView;
import platform.gwt.form2.client.navigator.GTreeNavigatorView;

public class GTreeNavigatorWindow extends GNavigatorWindow {
    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GTreeNavigatorView(this, navigatorController);
    }
}
