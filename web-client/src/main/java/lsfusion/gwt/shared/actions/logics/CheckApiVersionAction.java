package lsfusion.gwt.shared.actions.logics;

import lsfusion.gwt.shared.actions.RequestAction;
import lsfusion.gwt.shared.actions.navigator.NavigatorAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CheckApiVersionAction extends LogicsAction<StringResult> {
    public String message;

    public CheckApiVersionAction() {
    }

    public CheckApiVersionAction(String message) {
        this.message = message;
    }

}