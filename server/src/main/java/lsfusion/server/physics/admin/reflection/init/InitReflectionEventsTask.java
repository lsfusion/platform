package lsfusion.server.physics.admin.reflection.init;

import lsfusion.server.logics.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitReflectionEventsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing reflection events";
    }

    public void run(Logger logger) {
        getBL().initReflectionEvents();
    }
}
