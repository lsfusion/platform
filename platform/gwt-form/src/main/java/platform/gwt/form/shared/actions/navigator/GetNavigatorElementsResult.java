package platform.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.view.GNavigatorElement;

public class GetNavigatorElementsResult implements Result {
    public GNavigatorElement root;


    public GetNavigatorElementsResult() {
    }

    public GetNavigatorElementsResult(GNavigatorElement root) {
        this.root = root;
    }
}
