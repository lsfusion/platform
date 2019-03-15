package lsfusion.server.logics.classes.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassDataPropsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class tables";
    }

    public void run(Logger logger) {
        getBL().initClassDataProps();
    }
}
