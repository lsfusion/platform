package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.module.controller.init.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitTablesTask extends GroupModuleTask {

    protected boolean isGraph() {
        return true; // нужен граф, так как алгоритм include'а таблиц, меняет parents и поэтому например remove может стать раньше add
    }

    public String getCaption() {
        return "Initializing tables";
    }

    protected void runInnerTask(LogicsModule module) throws RecognitionException {
        module.initTables(getDBNamingPolicy());
    }
}
