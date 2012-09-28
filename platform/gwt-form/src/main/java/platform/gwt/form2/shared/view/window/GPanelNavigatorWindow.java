package platform.gwt.form2.shared.view.window;

import platform.gwt.form2.client.navigator.GINavigatorController;
import platform.gwt.form2.client.navigator.GNavigatorView;
import platform.gwt.form2.client.navigator.GPanelNavigatorView;

public class GPanelNavigatorWindow extends GNavigatorWindow {
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GPanelNavigatorView(this, navigatorController);
    }
}
