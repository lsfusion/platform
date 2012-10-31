package platform.gwt.form.shared.view.window;

import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GNavigatorView;
import platform.gwt.form.client.navigator.GToolbarNavigatorView;

public class GToolbarNavigatorWindow extends GNavigatorWindow {
    public int type;
    public boolean showSelect;

    public int verticalTextPosition;
    public int horizontalTextPosition;

    public int verticalAlignment;
    public int horizontalAlignment;

    public float alignmentY;
    public float alignmentX;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GToolbarNavigatorView(this, navigatorController);
    }
}
