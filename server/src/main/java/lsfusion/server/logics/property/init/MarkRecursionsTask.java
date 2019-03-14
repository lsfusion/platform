package lsfusion.server.logics.property.init;

import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class MarkRecursionsTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Looking for recursions in abstract actions / properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof Action) {
            if (property instanceof ListCaseAction && ((ListCaseAction) property).isAbstract()) {
                ((ListCaseAction) property).markRecursions();
            }
        }

        if (property instanceof Property) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                ((CaseUnionProperty) property).checkRecursions();
            }
        }
    }

}
