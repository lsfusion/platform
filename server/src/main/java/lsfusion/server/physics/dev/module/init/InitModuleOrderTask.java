package lsfusion.server.physics.dev.module.init;

import lsfusion.server.logics.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitModuleOrderTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing modules order";
    }

    public void run(Logger logger) {
        getBL().initModuleOrders();
    }
}
