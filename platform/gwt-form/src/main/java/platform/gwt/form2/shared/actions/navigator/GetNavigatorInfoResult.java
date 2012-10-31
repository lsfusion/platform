package platform.gwt.form2.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GAbstractWindow;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;

import java.util.ArrayList;
import java.util.List;

public class GetNavigatorInfoResult implements Result {
    public GNavigatorElement root;

    public ArrayList<GNavigatorWindow> navigatorWindows;

    public GAbstractWindow relevantForms;
    public GAbstractWindow relevantClasses;
    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

    public GetNavigatorInfoResult() {
    }

    public GetNavigatorInfoResult(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, List<GAbstractWindow> commonWindows) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;

        relevantForms = commonWindows.get(0);
        relevantClasses = commonWindows.get(1);
        log = commonWindows.get(2);
        status = commonWindows.get(3);
        forms = commonWindows.get(4);
    }
}
