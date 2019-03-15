package lsfusion.server.physics.dev.i18n.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitLocalizerTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Initializing localizer";
    }

    @Override
    public void run(Logger logger) {
        getBL().initLocalizer();
    }
}
