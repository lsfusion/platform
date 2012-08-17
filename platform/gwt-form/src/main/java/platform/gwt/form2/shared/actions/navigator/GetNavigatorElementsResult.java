package platform.gwt.form2.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.form2.shared.view.GNavigatorElement;

public class GetNavigatorElementsResult implements Result {
    public GNavigatorElement root;

    public GetNavigatorElementsResult() {
    }

    public GetNavigatorElementsResult(GNavigatorElement root) {
        this.root = root;
    }
}
