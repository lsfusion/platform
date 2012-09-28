package platform.gwt.form2.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.window.GNavigatorWindow;

public class GetNavigatorElementsResult implements Result {
    public GNavigatorElement root;
    public GNavigatorWindow[] navigatorWindows;

    public GetNavigatorElementsResult() {
    }

    public GetNavigatorElementsResult(GNavigatorElement root, GNavigatorWindow[] navigatorWindows) {
        this.root = root;
        this.navigatorWindows = navigatorWindows;
    }
}
