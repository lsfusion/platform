package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitFullSingleTablesTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing full tables with one key";
    }

    public void run(Logger logger) {
        getBL().initFullSingleTables(getDBNamingPolicy());
    }
}
