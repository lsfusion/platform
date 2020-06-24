package lsfusion.gwt.client.controller.remote.action;

import lsfusion.gwt.client.controller.remote.action.logics.LogicsAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CreateNavigatorAction extends LogicsAction<StringResult> {
    public Integer screenWidth;
    public Integer screenHeight;

    public CreateNavigatorAction() {
    }

    public CreateNavigatorAction(Integer screenWidth, Integer screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
}
