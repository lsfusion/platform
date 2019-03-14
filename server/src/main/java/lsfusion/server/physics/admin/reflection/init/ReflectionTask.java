package lsfusion.server.physics.admin.reflection.init;

import lsfusion.server.base.task.PublicTask;
import lsfusion.server.base.task.Task;
import lsfusion.server.physics.admin.reflection.ReflectionManager;

import java.util.Set;

public abstract class ReflectionTask extends PublicTask {

    private ReflectionManager reflectionManager;

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }

    public ReflectionManager getReflectionManager() {
        return reflectionManager;
    }

    public void setReflectionManager(ReflectionManager reflectionManager) {
        this.reflectionManager = reflectionManager;
    }
}
