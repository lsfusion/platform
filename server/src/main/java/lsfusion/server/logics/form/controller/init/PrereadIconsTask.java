package lsfusion.server.logics.form.controller.init;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.task.GroupSplitTask;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;
import org.apache.log4j.Logger;

import java.sql.SQLException;

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

    private SecurityManager securityManager;

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    protected DataSession createSession() throws SQLException {
        return getDbManager().createSession();
    }

    @Override
    protected ImSet<String> getObjects() {
        BusinessLogics BL = getBL();
        MSet<String> mImages = SetFact.mSet();

        AppServerImage.prereadBestIcons.set(mImages);
        try {
            boolean[] contexts;
            if(SystemProperties.lightStart) { // maybe inDevMode should be used, as well as in ExportRmiObject
                try(DataSession session = createSession()) {
                    contexts = new boolean[] {BL.systemEventsLM.useBootstrap.read(session, getSecurityManager().getDefaultLoginUser()) != null};
                } catch (SQLException | SQLHandledException e) {
                    throw Throwables.propagate(e);
                }
            } else {
                contexts = new boolean[] {true, false};
            }

            for(boolean useBootstrap : contexts) {
                ConnectionContext context = new ConnectionContext(useBootstrap, false);
                for (FormEntity form : BL.getAllForms()) // actually only interactive forms are needed, but there is no way to get them
                    form.prereadAutoIcons(context);

                for (NavigatorElement element : BL.getNavigatorElements())
                    element.getImage(context);

                for (ConcreteCustomClass customClass : BL.getConcreteCustomClasses())
                    customClass.fillIcons(mImages, context);
            }

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

    @Override
    public boolean isEndLoggable() {
        return true;
    }
}
