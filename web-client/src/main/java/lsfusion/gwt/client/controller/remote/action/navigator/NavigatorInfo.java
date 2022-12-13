package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.navigator.GNavigatorChanges;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GPropertyNavigator;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavigatorInfo implements Serializable {
    public GNavigatorElement root;

    public ArrayList<GNavigatorWindow> navigatorWindows;

    public GNavigatorChangesDTO navigatorChanges;

    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

    @SuppressWarnings("unused")
    public NavigatorInfo() {
    }

    public NavigatorInfo(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, GNavigatorChangesDTO navigatorChanges, List<GAbstractWindow> commonWindows) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        this.navigatorChanges = navigatorChanges;

        log = commonWindows.get(0);
        status = commonWindows.get(1);
        forms = commonWindows.get(2);
    }
}
