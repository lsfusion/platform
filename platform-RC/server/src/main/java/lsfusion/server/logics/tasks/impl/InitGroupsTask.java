package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.tasks.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

public class InitGroupsTask extends GroupModuleTask {

    protected boolean isGraph() {
        return true;
    }

    public String getCaption() {
        return "Initializing property groups";
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initGroups();
    }
}
