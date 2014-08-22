package lsfusion.server.logics.debug;

import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.actions.flow.JoinActionProperty;
import lsfusion.server.logics.property.actions.flow.ListActionProperty;
import lsfusion.server.logics.scripted.ScriptingActionProperty;

public enum ActionDelegationType {
    BEFORE_DELEGATE, IN_DELEGATE, AFTER_DELEGATE;

    public static ActionDelegationType of(ActionProperty property, boolean modifyContext) {
        if (property instanceof JoinActionProperty || property instanceof ScriptingActionProperty) {
            return IN_DELEGATE;
        } else if (property instanceof ListActionProperty) {
            if(modifyContext)
                return BEFORE_DELEGATE;
            else
                return null;
        } else {
            return AFTER_DELEGATE;
        }
    }
}
