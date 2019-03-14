package lsfusion.server.logics.tasks;

import lsfusion.server.base.task.Task;

import java.util.Set;

public abstract class SimpleBLTask extends BLTask {

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }
}
