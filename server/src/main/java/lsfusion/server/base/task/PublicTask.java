package lsfusion.server.base.task;

import lsfusion.base.BaseUtils;

import java.util.Set;

public abstract class PublicTask extends Task {

    private Set<PublicTask> dependencies;

    public Set<PublicTask> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<PublicTask> dependencies) {
        this.dependencies = dependencies;
    }

    protected static Set<Task> getAllDependencies(PublicTask task) {
        return BaseUtils.immutableCast(task.getDependencies());
    }

}
