package lsfusion.server.logics.event.init;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import org.apache.log4j.Logger;

// need this task for multithreaded links read
public class PrereadSessionLinksTask extends PrereadLinksTask {

    public String getCaption() {
        return "Reading local events links";
    }

    @Override
    protected ApplyFilter getApplyFilter() {
        return ApplyFilter.SESSION;
    }
}
