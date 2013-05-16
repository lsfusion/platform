package platform.server.form.navigator;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.server.context.AbstractContext;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsInstance;
import platform.server.remote.RemoteForm;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class RemoteNavigatorContext extends AbstractContext {
    private final RemoteNavigator navigator;

    public RemoteNavigatorContext(RemoteNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return navigator.logicsInstance;
    }

    public String getLogMessage() {
        return navigator.getLogMessage();
    }

    public void delayUserInteraction(ClientAction action) {
        navigator.delayUserInteraction(action);
    }

    @Override
    public Object[] requestUserInteraction(final ClientAction... actions) {
        return navigator.requestUserInteraction(actions);
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        return new FormInstance(formEntity, navigator.logicsInstance,
                                   sessionScope.isNewSession() ? session.createSession() : session,
                                   navigator.securityPolicy, navigator, navigator,
                                   navigator.getComputer(), navigator.getConnection(), mapObjects, isModal, sessionScope.isManageSession(),
                                   checkOnOk, showDrop, interactive, null);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm(formInstance, navigator.getExportPort(), navigator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
