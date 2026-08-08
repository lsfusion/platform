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
        return "Looking for recursions in properties";
    }

    @Override
    protected int getSplitCount() {
        return 10000;
    }

    protected void runTask(ActionOrProperty property) {
        if (property instanceof Property) {
            // the two passes are two different dependency graphs : this one follows an event's depends and stops at a PREV, and it is the graph
            // getRecDepends walks - which is why every property is a root here, a cycle in it needs no abstract property to exist. the global
            // marks keep the whole pass a single O(V + E) walk anyway
            ((Property<?>) property).checkRecursions(globalMarksWithoutPrev, false);

            // the other one follows PREV and stops at the events - that graph is not the one anything computes, so it stays rooted where it has
            // always been, at the abstract properties, rather than newly calling a PREV relation that blows no stack recursive
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract())
                ((Property<?>) property).checkRecursions(globalMarksWithPrev, true);
        }
    }
}
