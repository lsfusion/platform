package platform.server.auth;

import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;

public class ChangePropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

    public void deny(LP<?> lp) { deny(lp.property); }
}
