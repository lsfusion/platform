package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.controller.init.BLGroupSingleSplitTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.exec.db.table.ImplementTable;

public abstract class GroupTablesTask extends BLGroupSingleSplitTask<ImplementTable> {

    protected ImSet<ImplementTable> getObjects() {
        return getBL().LM.tableFactory.getImplementTables();
    }

}