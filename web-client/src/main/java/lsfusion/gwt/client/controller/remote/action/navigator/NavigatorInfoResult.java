package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;
import java.util.List;

public class NavigatorInfoResult implements Result {
    public GNavigatorElement root;

    public ArrayList<GNavigatorWindow> navigatorWindows;

    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

    public NavigatorInfoResult() {
    }

    public NavigatorInfoResult(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, List<GAbstractWindow> commonWindows) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        log = commonWindows.get(0);
        status = commonWindows.get(1);
        forms = commonWindows.get(2);
    }
}
