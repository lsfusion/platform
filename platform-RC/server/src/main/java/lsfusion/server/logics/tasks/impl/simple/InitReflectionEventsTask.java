package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitReflectionEventsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing reflection events";
    }

    public void run(Logger logger) {
        getBL().initReflectionEvents();
    }
}
