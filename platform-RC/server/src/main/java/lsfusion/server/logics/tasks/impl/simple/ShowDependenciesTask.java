package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class ShowDependenciesTask extends SimpleBLTask {

    public String getCaption() {
        return "Showing dependencies";
    }

    public void run(Logger logger) {
        getBL().showDependencies();
    }
}
