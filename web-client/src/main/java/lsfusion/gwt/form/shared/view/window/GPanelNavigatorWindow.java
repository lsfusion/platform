package lsfusion.gwt.form.shared.view.window;

import lsfusion.gwt.form.client.navigator.GINavigatorController;
import lsfusion.gwt.form.client.navigator.GNavigatorView;
import lsfusion.gwt.form.client.navigator.GPanelNavigatorView;

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
