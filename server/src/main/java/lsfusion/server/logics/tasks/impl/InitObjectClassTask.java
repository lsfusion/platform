package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitObjectClassTask extends SimpleBLTask {
    public String getCaption() {
        return "Initializing object class";
    }

    public void run(Logger logger) {
        getBL().initObjectClass();
    }
}
