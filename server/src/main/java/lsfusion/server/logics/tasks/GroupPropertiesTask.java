package lsfusion.server.logics.tasks;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.Property;

public abstract class GroupPropertiesTask extends GroupProgramTask {

    protected abstract void runTask(Property property);

    @Override
    protected Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks() {
        BusinessLogics<?> BL = (BusinessLogics<?>) getBL();
        final int splitCount = 1000;
        MCol<SingleProgramTask> mTasks = ListFact.mCol();
        ImMap<Integer, ImSet<Property>> groupProps = BL.getOrderProperties().getSet().mapValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i / splitCount;
            }
        }).groupValues();
        for (int i = 0, size = groupProps.size(); i < size; i++) {
            final int group = groupProps.getKey(i);
            final ImSet<Property> propSet = groupProps.getValue(i);

            mTasks.add(new SingleProgramTask() {
                @Override
                public String getCaption() {
                    return GroupPropertiesTask.this.getCaption() + " for props from " + (group * splitCount) + " to " + ((group + 1) * splitCount);
                }

                @Override
                public boolean isLoggable() {
                    return GroupPropertiesTask.this.isGroupLoggable();
                }

                public void run() {
                    for (Property prop : propSet) {
                        runTask(prop);
                    }
                }
            });
        }
        ImCol<SingleProgramTask> tasks = mTasks.immutableCol();
        return new Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>>(tasks, tasks);
    }

}
