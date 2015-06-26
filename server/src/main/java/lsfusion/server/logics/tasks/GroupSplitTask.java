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

// разбивает на группы и выполняет группами
public abstract class GroupSplitTask<T> extends GroupProgramTask {

    protected abstract void runGroupTask(ImSet<T> objSet);
    
    protected abstract ImSet<T> getObjects(BusinessLogics<?> BL);
    
    protected int getSplitCount() {
        return 1000;        
    }
    
    @Override
    protected Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks() {
        BusinessLogics<?> BL = (BusinessLogics<?>) getBL();
        final int splitCount = getSplitCount();
        MCol<SingleProgramTask> mTasks = ListFact.mCol();
        ImMap<Integer, ImSet<T>> groupProps = getObjects(BL).mapValues(new GetIndex<Integer>() {
            public Integer getMapValue(int i) {
                return i / splitCount;
            }
        }).groupValues();
        for (int i = 0, size = groupProps.size(); i < size; i++) {
            final int group = groupProps.getKey(i);
            final ImSet<T> objSet = groupProps.getValue(i);

            mTasks.add(new SingleProgramTask() {
                @Override
                public String getCaption() {
                    return GroupSplitTask.this.getCaption() + " for objects from " + (group * splitCount) + " to " + ((group + 1) * splitCount);
                }

                @Override
                public boolean isLoggable() {
                    return GroupSplitTask.this.isGroupLoggable();
                }

                public void run() {
                    runGroupTask(objSet);
                }
            });
        }
        ImCol<SingleProgramTask> tasks = mTasks.immutableCol();
        return new Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>>(tasks, tasks);
    }
}
