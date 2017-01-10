package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.ReflectionTask;
import org.apache.log4j.Logger;

public class ExecuteOnStartedTask extends ReflectionTask {

    public String getCaption() {
        return "Executing System.onStarted[] (first apply)";
    }

    public void run(Logger logger) {
        getReflectionManager().runOnStarted();
    }
}
