package lsfusion.server.logics.controller.init;

import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import org.apache.log4j.Logger;

public class PrereadCachesTask extends SimpleBLTask {

    private SecurityManager securityManager;

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public String getCaption() {
        return "Prereading properties graph";
    }

    public void run(Logger logger) {
        if (!SystemProperties.lightStart) {
            if (!Settings.get().isDisablePrereadCaches()) {
                getBL().prereadCaches();
            }
            if (!Settings.get().isDisablePrereadSecurityPolicies()) {
                getSecurityManager().prereadSecurityPolicies();
            }
        }
    }
}
