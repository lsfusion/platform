package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.tasks.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitIndicesTask extends GroupModuleTask {

    public String getCaption() {
        return "Initializing indices";
    }

    protected boolean isGraph() {
        return false;
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initIndexes();
    }
}
