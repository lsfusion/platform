package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class CheckNavigatorElementsTask extends SimpleBLTask {

    public void run(Logger logger) {
        getBL().getNavigator().check();
    }

    public String getCaption() {
        return "Checking navigator elements";
    }
}
