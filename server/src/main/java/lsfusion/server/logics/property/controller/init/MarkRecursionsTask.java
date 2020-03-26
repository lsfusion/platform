package lsfusion.server.logics.property.controller.init;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.util.Set;

public class MarkRecursionsTask extends GroupPropertiesTask {

    private Set<Action> actionMarks = new ConcurrentIdentityWeakHashSet<>();
    private Set<Property> propertyMarks = new ConcurrentIdentityWeakHashSet<>();

    public String getCaption() {
        return "Looking for recursions in abstract actions / properties";
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof Action) {
            if (property instanceof ListCaseAction && ((ListCaseAction) property).isAbstract()) {
                ((ListCaseAction) property).markRecursions(actionMarks);
            }
        }

        if (property instanceof Property) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                ((CaseUnionProperty) property).checkRecursions(propertyMarks);
            }
        }
    }

}
