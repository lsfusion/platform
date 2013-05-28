package platform.gwt.form.shared.view.window;

import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GNavigatorView;
import platform.gwt.form.shared.view.GNavigatorElement;

import java.util.ArrayList;
import java.util.List;

public abstract class GNavigatorWindow extends GAbstractWindow {
    public List<GNavigatorElement> elements = new ArrayList<GNavigatorElement>();

    public int type;
    public boolean drawRoot;
    public boolean drawScrollBars;

    public abstract GNavigatorView createView(GINavigatorController navigatorController);
}
