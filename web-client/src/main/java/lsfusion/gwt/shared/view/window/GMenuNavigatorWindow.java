package lsfusion.gwt.shared.view.window;

import lsfusion.gwt.client.form.navigator.GINavigatorController;
import lsfusion.gwt.client.form.navigator.GMenuNavigatorView;
import lsfusion.gwt.client.form.navigator.GNavigatorView;

public class GMenuNavigatorWindow extends GNavigatorWindow {
    public int showLevel;
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GMenuNavigatorView(this, navigatorController);
    }
}
