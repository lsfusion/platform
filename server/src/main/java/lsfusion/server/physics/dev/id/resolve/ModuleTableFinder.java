package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.exec.table.ImplementTable;

public class ModuleTableFinder extends ModuleSingleElementFinder<ImplementTable, Object> {
    @Override
    protected ImplementTable getElement(LogicsModule module, String simpleName, Object param) {
        return module.getTable(simpleName);
    }
}
