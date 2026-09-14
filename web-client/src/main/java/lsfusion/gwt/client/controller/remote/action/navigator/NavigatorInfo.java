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
    // every window that holds forms, the default one FIRST - a form lookup answers with the first window that has it
    public ArrayList<GAbstractWindow> formsWindows;
    public GAbstractWindow forms; // the one of them that is System.forms, which the window says by its name

    public List<GNavigatorScheduler> navigatorSchedulers;

    @SuppressWarnings("unused")
    public NavigatorInfo() {
    }

    public NavigatorInfo(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, GNavigatorChangesDTO navigatorChanges, GAbstractWindow log,
                         ArrayList<GAbstractWindow> formsWindows, List<GNavigatorScheduler> navigatorSchedulers) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        this.navigatorChanges = navigatorChanges;

        this.log = log;
        this.formsWindows = formsWindows;
        for (GAbstractWindow window : formsWindows)
            if (window.isSystemForms())
                forms = window;

        this.navigatorSchedulers = navigatorSchedulers;
    }
}
