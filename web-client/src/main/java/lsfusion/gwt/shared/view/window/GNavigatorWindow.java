package lsfusion.gwt.shared.view.window;

import lsfusion.gwt.client.navigator.GINavigatorController;
import lsfusion.gwt.client.navigator.GNavigatorView;
import lsfusion.gwt.shared.view.GNavigatorElement;

import java.util.ArrayList;
import java.util.List;

public abstract class GNavigatorWindow extends GAbstractWindow {
    public List<GNavigatorElement> elements = new ArrayList<>();

    public boolean drawRoot;
    public boolean drawScrollBars;

    public abstract GNavigatorView createView(GINavigatorController navigatorController);
}
