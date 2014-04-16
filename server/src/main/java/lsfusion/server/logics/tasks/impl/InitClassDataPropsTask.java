package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitClassDataPropsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class tables";
    }

    public void run() {
        getBL().initClassDataProps();
    }
}
