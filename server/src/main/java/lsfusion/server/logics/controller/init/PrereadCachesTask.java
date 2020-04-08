package lsfusion.server.logics.controller.init;

import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import org.apache.log4j.Logger;

public class PrereadCachesTask extends SimpleBLTask {

    public String getCaption() {
        return "Prereading properties graph";
    }

    public void run(Logger logger) {
        if (!SystemProperties.lightStart && !Settings.get().isDisablePrereadCaches()) {
            getBL().prereadCaches();
        }
    }
}