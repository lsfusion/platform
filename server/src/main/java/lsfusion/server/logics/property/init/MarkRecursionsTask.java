package lsfusion.server.logics.property.init;

import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.action.flow.ListCaseActionProperty;

public class MarkRecursionsTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Looking for recursions in abstract actions / properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof ActionProperty) {
            if (property instanceof ListCaseActionProperty && ((ListCaseActionProperty) property).isAbstract()) {
                ((ListCaseActionProperty) property).markRecursions();
            }
        }

        if (property instanceof Property) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                ((CaseUnionProperty) property).checkRecursions();
            }
        }
    }

}
