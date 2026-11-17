package lsfusion.server.logics.navigator.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class FinalizeNavigatorElementsTask extends SimpleBLTask {

    public void run(Logger logger) {
        getBL().getNavigator().finalizeAroundInit(getBL().LM);
    }

    public String getCaption() {
        return "Finalizing navigator elements";
    }
}
