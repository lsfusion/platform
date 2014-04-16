package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.ReflectionTask;

public class ResetConnectionStatusTask extends ReflectionTask {

    public String getCaption() {
        return "Resetting connection status";
    }

    public void run() {
        getReflectionManager().resetConnectionStatus();
    }
}
