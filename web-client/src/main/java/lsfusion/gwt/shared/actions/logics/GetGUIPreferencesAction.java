package lsfusion.gwt.shared.actions.logics;

import net.customware.gwt.dispatch.shared.general.StringResult;

public class GetGUIPreferencesAction extends LogicsAction<StringResult> {
    public String message;

    public GetGUIPreferencesAction() {
    }

    public GetGUIPreferencesAction(String message) {
        this.message = message;
    }

}