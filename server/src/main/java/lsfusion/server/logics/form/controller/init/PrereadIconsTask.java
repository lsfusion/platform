package lsfusion.server.logics.form.controller.init;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.task.GroupSplitTask;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;

public class PrereadIconsTask extends GroupSplitTask<String> {

    @Override
    protected int getSplitCount() {
        return 250;
    }

    private BusinessLogics BL;

    public BusinessLogics getBL() {
        return BL;
    }

    public void setBL(BusinessLogics BL) {
        this.BL = BL;
    }

    protected DBManager getDbManager() {
        return getBL().getDbManager();
    }

    @Override
    protected ImSet<String> getObjects() {
        BusinessLogics BL = getBL();
        MSet<String> mImages = SetFact.mSet();

        AppServerImage.prereadBestIcons.set(mImages);
        try {
            for (FormEntity form : BL.getAllForms())
                form.prereadAutoIcons(FormInstanceContext.CACHE(form));

            for(ConnectionContext context : new ConnectionContext[]{new ConnectionContext(true), new ConnectionContext(false)})
                for (NavigatorElement element : BL.getNavigatorElements())
                    element.getImage(context);

            for (ConcreteCustomClass customClass : BL.getConcreteCustomClasses())
                customClass.fillIcons(AppServerImage.prereadBestIcons.get());

            return mImages.immutable();
        } finally {
            AppServerImage.prereadBestIcons.set(null);
        }
    }

    @Override
    protected void runGroupTask(ImSet<String> objSet, Logger logger) {
        AppServerImage.prereadBestIcons(getBL(), getDbManager(), objSet);
    }

    @Override
    public String getCaption() {
        return "Prereading auto icons";
    }
}
