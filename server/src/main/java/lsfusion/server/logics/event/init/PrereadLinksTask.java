package lsfusion.server.logics.event.init;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import org.apache.log4j.Logger;

// need this task for multithreaded links read
public class PrereadLinksTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Reading property links";
    }

    protected void runTask(ActionOrProperty property) {
        BusinessLogics.prereadSortedLinks(property);
    }
}
