package lsfusion.gwt.shared.view.window;

import lsfusion.gwt.client.form.navigator.GINavigatorController;
import lsfusion.gwt.client.form.navigator.GNavigatorView;
import lsfusion.gwt.client.form.navigator.GTreeNavigatorView;

public class GTreeNavigatorWindow extends GNavigatorWindow {
    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GTreeNavigatorView(this, navigatorController);
    }
}
