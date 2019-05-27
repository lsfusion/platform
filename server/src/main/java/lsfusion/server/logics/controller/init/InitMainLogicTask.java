package lsfusion.server.logics.controller.init;

import com.google.common.base.Throwables;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.module.controller.init.GroupModuleTask;
import org.antlr.runtime.RecognitionException;

import java.io.FileNotFoundException;

public class InitMainLogicTask extends GroupModuleTask {

    protected boolean isGroupLoggable() {
        return true;
    }

    protected long getTaskComplexity(LogicsModule module) {
        return module.getModuleComplexity();
    }

    protected boolean isGraph() {
        return true;
    }

    public String getCaption() {
        return "Initializing main logic";
    }

    protected void runInnerTask(LogicsModule module) throws RecognitionException, FileNotFoundException {
        module.initMainLogic();
    }
}
