package lsfusion.server.logics.controller.init;

import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import org.apache.log4j.Logger;

public class PrereadSecurityPoliciesTask extends SimpleBLTask {

    private SecurityManager securityManager;

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public String getCaption() {
        return "Prereading security policies";
    }

    @Override
    public boolean isStartLoggable() {
        return isEnabled();
    }

    public void run(Logger logger) {
        if (isEnabled()) {
            getSecurityManager().prereadSecurityPolicies();
        }
    }

    private static boolean isEnabled() {
        return !SystemProperties.lightStart && !Settings.get().isDisablePrereadSecurityPolicies();
    }
}