package lsfusion.server.logics.classes.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitObjectClassTask extends SimpleBLTask {
    public String getCaption() {
        return "Initializing object class";
    }

    public void run(Logger logger) {
        getBL().initObjectClass();
    }
}
