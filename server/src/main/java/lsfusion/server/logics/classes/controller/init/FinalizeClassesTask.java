package lsfusion.server.logics.classes.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class FinalizeClassesTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Finalizing classes";
    }

    @Override
    public void run(Logger logger) {
        getBL().finalizeClasses();
    }
}