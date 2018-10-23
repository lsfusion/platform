package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.tasks.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitMetaGroupsAndClassesTask extends GroupModuleTask {

    protected boolean isGraph() {
        return true;
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initMetaGroupsAndClasses();
    }

    public String getCaption() {
        return "Initializing metacodes, property groups and classes";
    }
}
