package lsfusion.server.physics.admin.reflection.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;

public class InitReflectionEventsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing reflection events";
    }

    public void run(Logger logger) {
        getDbManager().initReflectionEvents();
    }
}
