package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class FinalizeCalcAbstractTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing abstract properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
            property.finalizeInit();
        }
    }
}
