package lsfusion.server.logics.controller.init;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;

public class ExecuteOnStartedTask extends ExecuteActionTask {

    @Override
    public String getCaption() {
        return "Executing onStarted action";
    }

    @Override
    protected LA getLA(BusinessLogics BL) {
        return BL.systemEventsLM.onStarted;
    }

    @Override
    public boolean isEndLoggable() {
        return true;
    }
}
