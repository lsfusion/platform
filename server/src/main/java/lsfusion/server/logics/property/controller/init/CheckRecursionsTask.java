package lsfusion.server.logics.property.controller.init;

import lsfusion.base.Pair;
import lsfusion.base.col.heavy.concurrent.weak.ConcurrentIdentityWeakHashSet;
import lsfusion.server.base.task.SingleProgramTask;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.util.HashSet;
import java.util.Set;

public class CheckRecursionsTask extends GroupPropertiesTask {

    private Set<Property> propertyMarks = new ConcurrentIdentityWeakHashSet<>();
//    private final Set<Property> propertyMarks = new HashSet<>();

    public String getCaption() {
        return "Looking for recursions in abstract properties";
    }
    
    @Override
    protected Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks() {
        long start = System.currentTimeMillis();
        Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> res = super.initTasks();
        System.out.println(getCaption() + ": " + (System.currentTimeMillis() - start) + "ms");
        return res;
    }
    
    protected void runTask(ActionOrProperty property) {
        if (property instanceof Property) {
            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                ((CaseUnionProperty) property).checkRecursions(propertyMarks);
            }
        }
    }
    
//    @Override
//    protected int getSplitCount() {
//        return Integer.MAX_VALUE;
//    }
    
    @Override
    public boolean isEndLoggable() {
        return true;
    }

}
