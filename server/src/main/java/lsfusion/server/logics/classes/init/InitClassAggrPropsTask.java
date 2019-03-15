package lsfusion.server.logics.classes.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassAggrPropsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class aggr props";
    }

    public void run(Logger logger) {
        getBL().initClassAggrProps();
    }
}
