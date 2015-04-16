package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitFullSingleTablesTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing full tables with single keys";
    }

    public void run() {
        getBL().initFullSingleTables();
    }
}
