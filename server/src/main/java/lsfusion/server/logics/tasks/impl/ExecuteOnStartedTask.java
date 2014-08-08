package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.ReflectionManager;
import lsfusion.server.logics.tasks.ReflectionTask;

public class ExecuteOnStartedTask extends ReflectionTask {

    public String getCaption() {
        return "Executing System.onStarted[] (first apply)";
    }

    public void run() {
        getReflectionManager().runOnStarted();
    }
}
