package lsfusion.server.logics.controller.init;

import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;

public class ExecuteImportIconsTask extends ExecuteActionTask {

    @Override
    public String getCaption() {
        return "Synchronizing icons";
    }

    @Override
    protected LA getLA(BusinessLogics BL) {
        return BL.iconLM.importIcons;
    }
}
