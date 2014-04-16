package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class FinalizeCalcAbstractTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing abstract properties";
    }

    protected void runTask(Property property) {
        if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract())
            property.finalizeInit();
    }
}
