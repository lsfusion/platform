package lsfusion.server.logics.tasks;

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
