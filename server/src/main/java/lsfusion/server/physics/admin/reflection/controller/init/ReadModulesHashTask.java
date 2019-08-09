package lsfusion.server.physics.admin.reflection.controller.init;

import org.apache.log4j.Logger;

public class ReadModulesHashTask extends ReflectionTask {

    @Override
    public String getCaption() {
        return "Reading modules hash";
    }

    @Override
    public void run(Logger logger) {
        getReflectionManager().readModulesHash();
    }
}