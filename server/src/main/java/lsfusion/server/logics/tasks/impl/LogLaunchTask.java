package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.ReflectionTask;

public class LogLaunchTask extends ReflectionTask {

    public String getCaption() {
        return "Logging launch (first apply)";
    }

    public void run() {
        getReflectionManager().logLaunch();
    }
}
