package lsfusion.server.logics.event.init;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class GetSessionPropertyListTask extends SimpleBLTask {

    public String getCaption() {
        return "Building local events list";
    }

    public void run(Logger logger) {
        getBL().getPropertyList(ApplyFilter.SESSION);
    }
}
