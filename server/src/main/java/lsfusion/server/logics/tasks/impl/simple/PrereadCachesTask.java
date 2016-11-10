package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.logics.tasks.SimpleBLTask;

public class PrereadCachesTask extends SimpleBLTask {

    public String getCaption() {
        return "Prereading properties graph";
    }

    public void run() {
        if (!SystemProperties.isDebug && !Settings.get().isDisablePrereadCaches()) {
            getBL().prereadCaches();
        }
    }
}
