package lsfusion.server.logics.tasks;

import lsfusion.base.BaseUtils;

import java.util.Set;

public class GroupTask extends PublicTask {

    public String getCaption() {
        return "Group task";
    }

    public boolean isLoggable() {
        return false;
    }

    public Set<Task> getAllDependencies() {
        return getAllDependencies(this);
    }

    public void run() {
    }
}
