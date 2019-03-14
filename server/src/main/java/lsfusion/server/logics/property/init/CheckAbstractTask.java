package lsfusion.server.logics.property.init;

import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.Property;

public class CheckAbstractTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Checking abstract";
    }

    protected void runTask(Property property) {
        if (property instanceof CaseUnionProperty) {
//            ((CaseUnionProperty) property).checkAbstract();
        }
    }
}
