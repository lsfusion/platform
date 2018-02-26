package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.SimpleBLTask;
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
