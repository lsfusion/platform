package lsfusion.gwt.form.shared.view.window;

import lsfusion.gwt.form.client.navigator.GINavigatorController;
import lsfusion.gwt.form.client.navigator.GNavigatorView;
import lsfusion.gwt.form.client.navigator.GTreeNavigatorView;

public class GTreeNavigatorWindow extends GNavigatorWindow {
    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GTreeNavigatorView(this, navigatorController);
    }
}
