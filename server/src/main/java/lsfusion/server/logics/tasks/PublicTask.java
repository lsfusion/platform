package lsfusion.server.logics.tasks;

import lsfusion.base.BaseUtils;

import java.util.Set;

public abstract class PublicTask extends Task {

    private Set<PublicTask> dependencies;

    public Set<PublicTask> getDependencies() {
        return dependencies;
    }
    
    protected static Set<Task> getAllDependencies(PublicTask task) {
        return BaseUtils.immutableCast(task.getDependencies());
    }

    public void setDependencies(Set<PublicTask> dependencies) {
        this.dependencies = dependencies;
    }
    
}
