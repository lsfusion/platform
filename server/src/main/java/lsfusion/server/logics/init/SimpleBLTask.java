package lsfusion.server.logics.init;

import lsfusion.server.base.task.Task;
import lsfusion.server.logics.init.BLTask;

import java.util.Set;

public abstract class SimpleBLTask extends BLTask {

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }
}
