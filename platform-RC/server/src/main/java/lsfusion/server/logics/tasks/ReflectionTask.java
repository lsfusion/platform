package lsfusion.server.logics.tasks;

import lsfusion.server.logics.ReflectionManager;

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
