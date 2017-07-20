package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassDataPropsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class tables";
    }

    public void run(Logger logger) {
        getBL().initClassDataProps();
    }
}
