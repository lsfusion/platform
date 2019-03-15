package lsfusion.server.physics.dev.module.init;

import com.google.common.base.Throwables;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

import java.io.IOException;

public class CreateModulesTask extends SimpleBLTask {
    public String getCaption() {
        return "Creating modules";
    }

    public void run(Logger logger) {
        try {
            getBL().createModules();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
