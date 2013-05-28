package platform.gwt.form.shared.view.window;

import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GNavigatorView;
import platform.gwt.form.client.navigator.GPanelNavigatorView;

public class GPanelNavigatorWindow extends GNavigatorWindow {
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GPanelNavigatorView(this, navigatorController);
    }
}
