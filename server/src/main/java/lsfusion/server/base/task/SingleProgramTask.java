package lsfusion.server.base.task;

import lsfusion.server.base.task.ProgramTask;
import lsfusion.server.base.task.Task;

import java.util.Set;

public abstract class SingleProgramTask extends ProgramTask {

    public long runTime = 0;

    protected SingleProgramTask() {
        dependsToProceed = 0;
    }

    public Set<Task> getAllDependencies() {
        throw new UnsupportedOperationException();
    }
}
