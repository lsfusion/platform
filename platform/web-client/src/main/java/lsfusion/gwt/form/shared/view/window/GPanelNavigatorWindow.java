package lsfusion.gwt.form.shared.view.window;

import lsfusion.gwt.form.client.navigator.GINavigatorController;
import lsfusion.gwt.form.client.navigator.GNavigatorView;
import lsfusion.gwt.form.client.navigator.GPanelNavigatorView;

public class GPanelNavigatorWindow extends GNavigatorWindow {
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GPanelNavigatorView(this, navigatorController);
    }
}
