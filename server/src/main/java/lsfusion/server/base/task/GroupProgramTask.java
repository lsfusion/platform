package lsfusion.server.base.task;

import lsfusion.base.Pair;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Set;

public abstract class GroupProgramTask extends PublicTask {

    protected Task preTask = new ProgramTask() {
        
        public Set<Task> getAllDependencies() {
            return PublicTask.getAllDependencies(GroupProgramTask.this);
        }

        public boolean isStartLoggable() {
            return GroupProgramTask.this.isPreLoggable();
        }

        public String getCaption() {
            return GroupProgramTask.this.getCaption();
        }

        public void run(Logger logger) {
            if (prerun()) {
                Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> tasks = initTasks();
                for (SingleProgramTask task : tasks.first) {
                    task.addDependency(this, false);
                }
                for (SingleProgramTask task : tasks.second) {
                    GroupProgramTask.this.addDependency(task);
                }
            }
        }
    };

    protected abstract Pair<Iterable<SingleProgramTask>, Iterable<SingleProgramTask>> initTasks();

    protected boolean prerun() {
        return true;
    }

    protected boolean isGroupLoggable() {
        return false;
    }

    protected boolean isPreLoggable() {
        return true;
    }

    @Override
    public boolean isStartLoggable() {
        return false;
    }

    public Set<Task> getAllDependencies() {
        return Collections.singleton(preTask);
    }

    public void run(Logger logger) {
    }
}
