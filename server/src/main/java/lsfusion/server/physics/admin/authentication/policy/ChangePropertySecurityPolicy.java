package lsfusion.server.physics.admin.authentication.policy;

import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class ChangePropertySecurityPolicy extends AbstractSecurityPolicy<ActionOrProperty> {

    public void deny(LP<?, ?> lp) {
        if (lp != null)
            deny(lp.property);
    }
    public void deny(LP<?, ?>... lps) { for (LP<?, ?> lp : lps) deny(lp); }
}
