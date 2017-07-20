package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.tasks.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

// метакоды и namespace'ы парсятся
public class InitModuleRestTask extends GroupModuleTask {

    protected boolean isGraph() {
        return true; // для метакодов
    }

    protected void runTask(LogicsModule module) throws RecognitionException {
        module.initModule();
    }

    public String getCaption() {
        return "Initializing metacodes and namespaces";
    }
}
