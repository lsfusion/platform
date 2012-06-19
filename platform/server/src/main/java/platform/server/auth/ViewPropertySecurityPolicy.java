package platform.server.auth;

import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;

public class ViewPropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

    public void deny(LP<?, ?> lp) {
        if (lp != null)
            deny(lp.property);
    }
}
