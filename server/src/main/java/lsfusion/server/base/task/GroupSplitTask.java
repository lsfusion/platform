package lsfusion.server.base.task;

import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclSet;
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

        // slices taken by index : mapping every object to its chunk number and grouping by that built a map the size of the whole
        // set and hashed every element, to say what the index arithmetic says directly
        ImSet<T> objects = getObjects();
        for (int start = 0, size = objects.size(); start < size; start += splitCount) {
            final int from = start;
            int end = Math.min(start + splitCount, size);

            MExclSet<T> mObjSet = SetFact.mExclSet(end - start);
            for (int j = start; j < end; j++)
                mObjSet.exclAdd(objects.get(j));
            final ImSet<T> objSet = mObjSet.immutable();

            mTasks.add(new SingleProgramTask() {
                @Override
                public String getCaption() {
                    return GroupSplitTask.this.getCaption() + " for objects from " + from + " to " + (from + splitCount);
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
