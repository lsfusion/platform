package lsfusion.server.logics.tasks;

import lsfusion.base.BaseUtils;

import java.util.Set;

public abstract class SimpleBLTask extends BLTask {

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }
}
