package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class ShowDependenciesTask extends SimpleBLTask {

    public String getCaption() {
        return "Showing dependencies";
    }

    public void run(Logger logger) {
        getBL().showDependencies();
    }
}
