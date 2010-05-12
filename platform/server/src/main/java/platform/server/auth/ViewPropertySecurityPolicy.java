package platform.server.auth;

import platform.server.logics.linear.LC;
import platform.server.logics.control.Control;

public class ViewPropertySecurityPolicy extends AbstractSecurityPolicy<Control> {

    public void deny(LC<?,?> lp) { deny(lp.property); }
}
