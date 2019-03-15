package lsfusion.server.logics.controller.init;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.module.init.GroupModuleTask;
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
