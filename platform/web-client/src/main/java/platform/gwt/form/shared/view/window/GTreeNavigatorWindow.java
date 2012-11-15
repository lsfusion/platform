package platform.gwt.form.shared.view.window;

import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GNavigatorView;
import platform.gwt.form.client.navigator.GTreeNavigatorView;

public class GTreeNavigatorWindow extends GNavigatorWindow {
    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GTreeNavigatorView(this, navigatorController);
    }
}
