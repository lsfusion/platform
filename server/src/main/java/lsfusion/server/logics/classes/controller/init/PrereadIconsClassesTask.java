package lsfusion.server.logics.classes.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import org.apache.log4j.Logger;

public class PrereadIconsClassesTask extends GroupClassesTask {

    @Override
    public String getCaption() {
        return "Prereading auto icons in static objects";
    }

    @Override
    protected void runTask(CustomClass object) {
        if(object instanceof ConcreteCustomClass)
            ((ConcreteCustomClass) object).fillIcons(AppServerImage.prereadBestIcons.get());
    }

    @Override
    protected void runGroupTask(ImSet<CustomClass> objSet, Logger logger) {
        AppServerImage.prereadBestIcons(getBL(), getDbManager(), () -> {
            super.runGroupTask(objSet, logger);
        });
    }

}
