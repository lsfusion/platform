package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitFullSingleTablesTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing full tables with single keys";
    }

    public void run(Logger logger) {
        getBL().initFullSingleTables();
    }
}
