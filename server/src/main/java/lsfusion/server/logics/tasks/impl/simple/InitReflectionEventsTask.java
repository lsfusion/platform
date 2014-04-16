package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitReflectionEventsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing reflection events";
    }

    public void run() {
        getBL().initReflectionEvents();
    }
}
