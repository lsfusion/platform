package lsfusion.server.logics.form.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import org.apache.log4j.Logger;

public class FinalizeGroupsTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Finalizing groups";
    }

    @Override
    public void run(Logger logger) {
        getBL().finalizeGroups();
    }
}