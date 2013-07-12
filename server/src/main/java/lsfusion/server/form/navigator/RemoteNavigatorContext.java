package lsfusion.server.form.navigator;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;

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
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters) throws SQLException {
        return new FormInstance(formEntity, navigator.logicsInstance,
                                   sessionScope.isNewSession() ? session.createSession() : session,
                                   navigator.securityPolicy, navigator, navigator,
                                   navigator.getComputer(), navigator.getConnection(), mapObjects, isModal, sessionScope.isManageSession(),
                                   checkOnOk, showDrop, interactive, contextFilters);
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
