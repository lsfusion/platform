package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import lsfusion.gwt.base.shared.actions.RequestAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CheckApiVersionAction extends RequestAction<StringResult> implements NavigatorAction {
    public String message;

    public CheckApiVersionAction() {
    }

    public CheckApiVersionAction(String message) {
        this.message = message;
    }

}