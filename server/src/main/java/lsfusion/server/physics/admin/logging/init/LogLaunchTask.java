package lsfusion.server.physics.admin.logging.init;

import lsfusion.server.physics.admin.reflection.init.ReflectionTask;
import org.apache.log4j.Logger;

public class LogLaunchTask extends ReflectionTask {

    public String getCaption() {
        return "Logging launch";
    }

    public void run(Logger logger) {
        getReflectionManager().logLaunch();
    }
}
