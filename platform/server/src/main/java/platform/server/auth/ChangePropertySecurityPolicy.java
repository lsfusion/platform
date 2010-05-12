package platform.server.auth;

import platform.server.logics.property.Property;
import platform.server.logics.linear.LP;

public class ChangePropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

    public void deny(LP<?> lp) { deny(lp.property); }
}
