package lsfusion.server.physics.dev.module.init;

import lsfusion.server.logics.LogicsModule;
import org.antlr.runtime.RecognitionException;

public class InitModuleDependenciesTask extends GroupModuleTask {

    public String getCaption() {
        return "Initializing dependencies";
    }

    protected boolean isGraph() {
        return false;
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initModuleDependencies();
    }
}
