package lsfusion.server.logics.controller.init;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;

public class ExecuteOnInitTask extends ExecuteActionTask {
    @Override
    protected LA getLA(BusinessLogics BL) {
        return BL.systemEventsLM.onInit;
    }

    @Override
    public String getCaption() {
        return "Executing onInit action";
    }
}
