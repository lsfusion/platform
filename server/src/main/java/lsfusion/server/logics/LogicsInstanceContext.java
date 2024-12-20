package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ResetServerSettingsCacheClientAction;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.authentication.security.controller.manager.SecurityManager;
import lsfusion.server.physics.admin.log.LogInfo;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Locale;

import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

public class LogicsInstanceContext extends AbstractContext {
    private static final Logger logger = Logger.getLogger(LogicsInstanceContext.class);

    private final LogicsInstance logicsInstance;

    private final ConnectionContext remoteContext;

    public LogicsInstanceContext(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;

        remoteContext = new ConnectionContext(true, false, false);
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public FocusListener getFocusListener() {
        return null;
    }

    public CustomClassListener getClassListener() {
        return null;
    }

    public Long getCurrentComputer() {
        return logicsInstance.getDbManager().getServerComputer();
    }

    public Long getCurrentConnection() {
        return null;
    }

    public Long getCurrentUser() {
        return logicsInstance.getDbManager().getSystemUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public LocalePreferences getLocalePreferences() {
        return null;
    }

    @Override
    public LogInfo getLogInfo() {
        return LogInfo.system;
    }

    // used in some deprecated actions
    @Deprecated
    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        assert false;
        return new FormInstance(formEntity, getLogicsInstance(), inputObjects,
                session,
                SecurityManager.baseServerSecurityPolicy, getFocusListener(), getClassListener(),
                mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, type, false, contextFilters, readonly, getLocale());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return remoteContext;
    }
}
