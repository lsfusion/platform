package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitModuleOrderTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing modules order";
    }

    public void run(Logger logger) {
        getBL().initModuleOrders();
    }
}
