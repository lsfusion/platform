package lsfusion.server.logics.event.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.ApplyFilter;
import lsfusion.server.logics.property.controller.init.GroupPropertiesTask;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import org.apache.log4j.Logger;

public abstract class PrereadLinksTask extends GroupPropertiesTask {

    protected abstract ApplyFilter getApplyFilter();

    @Override
    protected ImSet<ActionOrProperty> getObjects() {
        return super.getObjects().filterFn(getApplyFilter()::contains);
    }

    @Override
    protected boolean prerun() {
        getBL().fillActionChangeProps(getApplyFilter());
        return true;
    }

    protected void runTask(ActionOrProperty property) {
        BusinessLogics.prereadSortedLinks(property);
    }

    @Override
    public void run(Logger logger) {
        getBL().dropActionChangeProps(getApplyFilter());
    }
}