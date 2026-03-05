package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.GNavigatorChangesDTO;
import lsfusion.gwt.client.GNavigatorScheduler;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NavigatorInfo implements Serializable {
    public GNavigatorElement root;

    public ArrayList<GNavigatorWindow> navigatorWindows;

    public GNavigatorChangesDTO navigatorChanges;

    public GAbstractWindow log;
    public GAbstractWindow forms;

    public List<GNavigatorScheduler> navigatorSchedulers;

    @SuppressWarnings("unused")
    public NavigatorInfo() {
    }

    public NavigatorInfo(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, GNavigatorChangesDTO navigatorChanges, List<GAbstractWindow> commonWindows,
                         List<GNavigatorScheduler> navigatorSchedulers) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        this.navigatorChanges = navigatorChanges;

        log = commonWindows.get(0);
        forms = commonWindows.get(1);

        this.navigatorSchedulers = navigatorSchedulers;
    }
}
