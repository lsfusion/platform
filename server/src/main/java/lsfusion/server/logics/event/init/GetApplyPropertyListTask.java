package lsfusion.server.logics.event.init;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class GetApplyPropertyListTask extends SimpleBLTask {

    public String getCaption() {
        return "Building global events / materialized properties list";
    }

    public void run(Logger logger) {
        BusinessLogics BL = getBL();
        BL.propertyListInitialized = true;
        BL.getPropertyList(ApplyFilter.NO);
    }
}
