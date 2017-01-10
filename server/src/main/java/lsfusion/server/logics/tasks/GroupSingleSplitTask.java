package lsfusion.server.logics.tasks;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.stack.ExecutionStackAspect;
import org.apache.log4j.Logger;

public abstract class GroupSingleSplitTask<T> extends GroupSplitTask<T> {

    protected abstract void runTask(T object);

    protected boolean ignoreTaskException() {
        return false;
    }

    @Override
    protected void runGroupTask(ImSet<T> objSet, Logger logger) {
        for(T obj : objSet) {
            try {
                runTask(obj);
            } catch (Exception e) {
                if(ignoreTaskException()) {
                    logger.error(ExecutionStackAspect.getExceptionStackString());
                    logger.error("RunGroupTask Error:", e);
                }
                else throw Throwables.propagate(e);
            }
        }
    }
}
