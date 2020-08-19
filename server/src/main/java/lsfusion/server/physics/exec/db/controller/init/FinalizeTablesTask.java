package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.physics.exec.db.table.ImplementTable;
import org.apache.log4j.Logger;

public class FinalizeTablesTask extends GroupTablesTask {

    @Override
    protected void runTask(ImplementTable table) {
        table.finalizeAroundInit();
    }

    @Override
    public String getCaption() {
        return "Finalizing tables";
    }
}
