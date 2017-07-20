package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.ReflectionTask;
import org.apache.log4j.Logger;

public class ResetConnectionStatusTask extends ReflectionTask {

    public String getCaption() {
        return "Resetting connection status";
    }

    public void run(Logger logger) {
        getReflectionManager().resetConnectionStatus();
    }
}
