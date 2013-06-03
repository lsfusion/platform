package lsfusion.server.auth;

import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.Property;

public class ViewPropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

    public void deny(LP<?, ?> lp) {
        if (lp != null)
            deny(lp.property);
    }
}
