package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.ReflectionTask;
import org.apache.log4j.Logger;

public class LogLaunchTask extends ReflectionTask {

    public String getCaption() {
        return "Logging launch";
    }

    public void run(Logger logger) {
        getReflectionManager().logLaunch();
    }
}
