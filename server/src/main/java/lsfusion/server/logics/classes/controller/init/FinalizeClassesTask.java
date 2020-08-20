package lsfusion.server.logics.classes.controller.init;

import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class FinalizeClassesTask extends GroupClassesTask {

    @Override
    public String getCaption() {
        return "Finalizing classes";
    }

    @Override
    protected void runTask(CustomClass customClass) {
        customClass.finalizeAroundInit();
    }
}