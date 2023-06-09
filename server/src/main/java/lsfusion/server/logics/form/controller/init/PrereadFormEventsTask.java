package lsfusion.server.logics.form.controller.init;

import lsfusion.server.logics.form.struct.FormEntity;

public class PrereadFormEventsTask extends GroupFormsTask {

    @Override
    protected boolean prerun() {
        getBL().getCheckConstrainedProperties(); // to avoid the concurrent calculation
        return true;
    }

    protected void runTask(FormEntity form) {
        form.prereadEventActions();
    }

    public String getCaption() {
        return "Prereading form property events";
    }

}
