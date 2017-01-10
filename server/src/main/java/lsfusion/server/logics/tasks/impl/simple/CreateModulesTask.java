package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.server.logics.tasks.SimpleBLTask;
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
