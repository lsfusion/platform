package lsfusion.server.logics.tasks;

import java.util.Set;

public abstract class SimpleBLTask extends BLTask {

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }
}
