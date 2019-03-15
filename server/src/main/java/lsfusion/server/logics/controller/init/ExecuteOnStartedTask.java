package lsfusion.server.logics.controller.init;

import lsfusion.server.physics.admin.reflection.controller.init.ReflectionTask;
import org.apache.log4j.Logger;

public class ExecuteOnStartedTask extends ReflectionTask {

    public String getCaption() {
        return "Executing System.onStarted[] (first apply)";
    }

    public void run(Logger logger) {
        getReflectionManager().runOnStarted();
    }
}
