package lsfusion.server.base.task;

import org.apache.log4j.Logger;

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

    public void run(Logger logger) {
    }
}
