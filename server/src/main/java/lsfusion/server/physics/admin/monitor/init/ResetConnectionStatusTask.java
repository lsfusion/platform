package lsfusion.server.physics.admin.monitor.init;

import lsfusion.server.physics.admin.reflection.init.ReflectionTask;
import org.apache.log4j.Logger;

public class ResetConnectionStatusTask extends ReflectionTask {

    public String getCaption() {
        return "Resetting connection status";
    }

    public void run(Logger logger) {
        getReflectionManager().resetConnectionStatus();
    }
}
