package lsfusion.server.logics.action.init;

import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.property.oraction.Property;
import lsfusion.server.logics.action.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.init.GroupPropertiesTask;

public class FinalizeActionAbstractTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Finalizing abstract actions";
    }

    protected void runTask(Property property) {
        if (property instanceof ActionProperty) {
            if (property instanceof ListCaseActionProperty && ((ListCaseActionProperty) property).isAbstract()) {
                property.finalizeInit();
            }
        }
    }
}
