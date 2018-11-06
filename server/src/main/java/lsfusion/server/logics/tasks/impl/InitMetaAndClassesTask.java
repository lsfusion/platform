package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.tasks.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitMetaAndClassesTask extends GroupModuleTask {

    protected boolean isGraph() {
        return true;
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initMetaAndClasses();
    }

    public String getCaption() {
        return "Initializing metacodes and classes";
    }
}
