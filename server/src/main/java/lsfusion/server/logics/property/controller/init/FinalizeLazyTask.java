package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.property.LazyProperty;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class FinalizeLazyTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing lazy properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof LazyProperty) {
            property.finalizeInit();
        }
    }
}
