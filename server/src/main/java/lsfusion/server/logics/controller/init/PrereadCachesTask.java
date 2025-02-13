package lsfusion.server.logics.controller.init;

import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import org.apache.log4j.Logger;

public class PrereadCachesTask extends SimpleBLTask {

    public String getCaption() {
        return "Prereading properties graph";
    }

    @Override
    public boolean isStartLoggable() {
        return isEnabled();
    }

    public void run(Logger logger) {
        if (isEnabled()) {
            getBL().prereadCaches();
        }
    }

    private boolean isEnabled() {
        return !SystemProperties.lightStart && !Settings.get().isDisablePrereadCaches();
    }
}