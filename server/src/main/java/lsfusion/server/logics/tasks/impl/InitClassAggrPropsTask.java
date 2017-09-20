package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassAggrPropsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class aggr props";
    }

    public void run(Logger logger) {
        getBL().initClassAggrProps();
    }
}
