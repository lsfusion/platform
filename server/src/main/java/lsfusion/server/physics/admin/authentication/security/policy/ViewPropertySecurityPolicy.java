package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class ViewPropertySecurityPolicy extends AbstractSecurityPolicy<ActionOrProperty> {

    public void deny(LAP<?, ?> lp) {
        if (lp != null)
            deny(lp.property);
    }
}
