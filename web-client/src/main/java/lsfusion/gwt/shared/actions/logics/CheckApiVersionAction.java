package lsfusion.gwt.shared.actions.logics;

import net.customware.gwt.dispatch.shared.general.StringResult;

public class CheckApiVersionAction extends LogicsAction<StringResult> {
    public String message;

    public CheckApiVersionAction() {
    }

    public CheckApiVersionAction(String message) {
        this.message = message;
    }

}