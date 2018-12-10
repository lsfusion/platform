package lsfusion.gwt.shared.form.view.window;

import lsfusion.gwt.client.form.navigator.GINavigatorController;
import lsfusion.gwt.client.form.navigator.GNavigatorView;
import lsfusion.gwt.client.form.navigator.GPanelNavigatorView;

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
