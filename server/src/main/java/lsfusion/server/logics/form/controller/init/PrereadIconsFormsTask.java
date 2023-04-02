package lsfusion.server.logics.form.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.form.struct.FormEntity;
import org.apache.log4j.Logger;

public class PrereadIconsFormsTask extends GroupFormsTask {

    @Override
    protected int getSplitCount() {
        return 100;
    }

    @Override
    public String getCaption() {
        return "Prereading auto icons on forms";
    }

    @Override
    protected void runTask(FormEntity form) {
        form.prereadAutoIcons();
    }

    @Override
    protected void runGroupTask(ImSet<FormEntity> objSet, Logger logger) {
        AppServerImage.prereadBestIcons(getBL(), getDbManager(), () -> super.runGroupTask(objSet, logger));
    }
}
