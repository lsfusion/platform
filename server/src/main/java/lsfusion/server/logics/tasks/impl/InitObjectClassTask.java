package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitObjectClassTask extends SimpleBLTask {
    public String getCaption() {
        return "Initializing object class";
    }

    public void run() {
        getBL().initObjectClass();
    }
}
