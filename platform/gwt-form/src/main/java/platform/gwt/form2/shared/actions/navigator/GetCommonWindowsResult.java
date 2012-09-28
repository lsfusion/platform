package platform.gwt.form2.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.window.GAbstractWindow;

import java.util.List;

public class GetCommonWindowsResult implements Result {
    public GAbstractWindow relevantForms;
    public GAbstractWindow relevantClasses;
    public GAbstractWindow log;
    public GAbstractWindow status;
    public GAbstractWindow forms;

    public GetCommonWindowsResult(List<GAbstractWindow> windows) {
        relevantForms = windows.get(0);
        relevantClasses = windows.get(1);
        log = windows.get(2);
        status = windows.get(3);
        forms = windows.get(4);
    }

    public GetCommonWindowsResult() {
    }
}
