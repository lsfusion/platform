package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.property.CaseUnionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

public class CheckAbstractTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Checking abstract";
    }

    protected void runTask(Property property) {
        if(property instanceof CaseUnionProperty) {
            ((CaseUnionProperty)property).checkAbstract();
        }
    }
}
