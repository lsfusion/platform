package lsfusion.server.physics.exec.db.controller.init;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.module.controller.init.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitIndicesTask extends GroupModuleTask {

    public String getCaption() {
        return "Initializing indices";
    }

    protected boolean isGraph() {
        return false;
    }

    protected void runInnerTask(LogicsModule module) throws RecognitionException {
        module.initIndexes();
    }
}
