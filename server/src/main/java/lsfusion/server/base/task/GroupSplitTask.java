package lsfusion.server.base.task;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MCol;
import org.apache.log4j.Logger;

// разбивает на группы и выполняет группами
public abstract class GroupSplitTask<T> extends GroupProgramTask {

    protected abstract void runGroupTask(ImSet<T> objSet, Logger logger);
    
    protected abstract ImSet<T> getObjects();
    
    protected int getSplitCount() {
        return 100;
    }
    
    @Override
    protected Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks() {
        final int splitCount = getSplitCount();
        MCol<SingleProgramTask> mTasks = ListFact.mCol();
        ImSet<T> objects = getObjects();
        ImMap<Integer, ImSet<T>> groupProps = objects.mapValues((int i) -> i / splitCount).groupValues();
        System.out.printf("%s: SplitCount: %d, groups count: %d, all: %d%n", getCaption(), splitCount, groupProps.size(), objects.size());
        for (int i = 0, size = groupProps.size(); i < size; i++) {
            final int group = groupProps.getKey(i);
            final ImSet<T> objSet = groupProps.getValue(i);

            mTasks.add(new SingleProgramTask() {
                @Override
                public String getCaption() {
                    return GroupSplitTask.this.getCaption() + " for objects from " + (group * splitCount) + " to " + ((group + 1) * splitCount);
                }

                @Override
                public boolean isStartLoggable() {
                    return GroupSplitTask.this.isGroupLoggable();
                }

                public void run(Logger logger) {
                    runGroupTask(objSet, logger);
                }
            });
        }
        ImCol<SingleProgramTask> tasks = mTasks.immutableCol();
        return new Pair<>(tasks, tasks);
    }
}
