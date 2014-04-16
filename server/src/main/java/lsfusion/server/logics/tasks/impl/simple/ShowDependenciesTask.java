package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class ShowDependenciesTask extends SimpleBLTask {

    public String getCaption() {
        return "Showing dependencies";
    }

    public void run() {
        getBL().showDependencies();
    }
}
