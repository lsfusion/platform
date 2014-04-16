package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitModuleOrderTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing modules order";
    }

    public void run() {
        getBL().initModuleOrders();
    }
}
