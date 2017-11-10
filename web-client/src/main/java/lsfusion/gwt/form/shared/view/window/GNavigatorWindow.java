package lsfusion.gwt.form.shared.view.window;

import lsfusion.gwt.form.client.navigator.GINavigatorController;
import lsfusion.gwt.form.client.navigator.GNavigatorView;
import lsfusion.gwt.form.shared.view.GNavigatorElement;

import java.util.ArrayList;
import java.util.List;

public abstract class GNavigatorWindow extends GAbstractWindow {
    public List<GNavigatorElement> elements = new ArrayList<>();

    public int type;
    public boolean drawRoot;
    public boolean drawScrollBars;

    public abstract GNavigatorView createView(GINavigatorController navigatorController);
}
