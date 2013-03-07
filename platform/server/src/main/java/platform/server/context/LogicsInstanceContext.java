package platform.server.context;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsInstance;
import platform.server.logics.SecurityManager;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class LogicsInstanceContext extends AbstractContext {
    private final LogicsInstance logicsInstance;

    public LogicsInstanceContext(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        DataObject serverComputer = logicsInstance.getDbManager().getServerComputerObject();
        return new FormInstance(formEntity,
                                logicsInstance, session, SecurityManager.serverSecurityPolicy, null, null,
                                serverComputer,
                                null, mapObjects, isModal, sessionScope.isManageSession(), checkOnOk, showDrop, interactive, null);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm(formInstance, logicsInstance.getRmiManager().getExportPort(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
