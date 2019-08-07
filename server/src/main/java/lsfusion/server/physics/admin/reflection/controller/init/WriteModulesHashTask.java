package lsfusion.server.physics.admin.reflection.controller.init;

import org.apache.log4j.Logger;

public class WriteModulesHashTask extends ReflectionTask {

    @Override
    public String getCaption() {
        return "Writing modules hash";
    }

    @Override
    public void run(Logger logger) {
        getReflectionManager().writeModulesHash();
    }
}
