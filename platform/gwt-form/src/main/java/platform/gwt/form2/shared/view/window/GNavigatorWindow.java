package platform.gwt.form2.shared.view.window;

import platform.gwt.form2.client.navigator.GINavigatorController;
import platform.gwt.form2.client.navigator.GNavigatorView;
import platform.gwt.form2.shared.view.GNavigatorElement;

import java.util.ArrayList;
import java.util.List;

public abstract class GNavigatorWindow extends GAbstractWindow {
    public List<GNavigatorElement> elements = new ArrayList<GNavigatorElement>();

    public int type;
    public boolean drawRoot;
    public boolean drawScrollBars;

    public abstract GNavigatorView createView(GINavigatorController navigatorController);
}
