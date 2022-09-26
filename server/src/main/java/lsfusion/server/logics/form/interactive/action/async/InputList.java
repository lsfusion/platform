package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import org.apache.commons.lang3.ArrayUtils;

import java.io.Serializable;

public class InputList implements Serializable {

    public final InputListAction[] actions;
    public final boolean strict;

    public InputList(InputListAction[] actions, boolean strict) {
        this.actions = actions;
        this.strict = strict;
    }

    public InputList filter(SecurityPolicy policy, ActionOrProperty securityProperty) {
        if (policy != null) {
            for (int i = 0; i < actions.length; i++) {
                if (actions[i].action.equals("new")) {
                    if (!policy.checkPropertyEditObjectsPermission(securityProperty)) {
                        return new InputList(ArrayUtils.remove(actions, i), strict);
                    }
                    break;
                }
            }
        }
        return this;
    }
}
