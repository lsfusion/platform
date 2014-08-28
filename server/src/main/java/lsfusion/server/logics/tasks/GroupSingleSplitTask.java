package lsfusion.server.logics.tasks;

import lsfusion.base.col.interfaces.immutable.ImSet;

public abstract class GroupSingleSplitTask<T> extends GroupSplitTask<T> {

    protected abstract void runTask(T object);

    @Override
    protected void runGroupTask(ImSet<T> objSet) {
        for(T obj : objSet)
            runTask(obj);
    }
}
