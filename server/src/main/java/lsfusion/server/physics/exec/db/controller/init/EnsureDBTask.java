package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class EnsureDBTask extends SimpleBLTask {

    public String getCaption() {
        return "Ensuring database";
    }

    public void run(Logger logger) {
        getDbManager().ensureDB();
    }
}
