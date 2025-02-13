package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CheckRecursionsTask extends GroupPropertiesTask {

    private final Set<Property<?>> globalMarksWithoutPrev = ConcurrentHashMap.newKeySet();
    private final Set<Property<?>> globalMarksWithPrev = ConcurrentHashMap.newKeySet();

    public String getCaption() {
        return "Looking for recursions in abstract properties";
    }
    
    @Override
    protected int getSplitCount() {
        return 10000;
    }
    
    protected void runTask(ActionOrProperty property) {
        if (property instanceof Property) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                // We run it twice to eliminate loops that contain both PREV and events/CHANGED simultaneously.
                ((CaseUnionProperty) property).checkRecursions(globalMarksWithoutPrev, false);
                ((CaseUnionProperty) property).checkRecursions(globalMarksWithPrev, true);
            }
        }
    }
}
