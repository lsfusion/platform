package lsfusion.server.logics.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.table.ImplementTable;

public class ModuleTableFinder extends ModuleSingleElementFinder<ImplementTable, Object> {
    @Override
    protected ImplementTable getElement(LogicsModule module, String simpleName, Object param) {
        return module.getTable(simpleName);
    }
}
