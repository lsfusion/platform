package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.property.ActionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class FinalizeActionAbstractTask extends GroupPropertiesTask {
    public String getCaption() {
        return "Finalizing abstract actions";
    }

    protected void runTask(Property property) {
        if(property instanceof ActionProperty) {
            if(property instanceof ListCaseActionProperty && ((ListCaseActionProperty)property).isAbstract())
                property.finalizeInit();
        }
    }
}
