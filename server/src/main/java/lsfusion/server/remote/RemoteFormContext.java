package lsfusion.server.remote;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class RemoteFormContext<T extends BusinessLogics<T>, F extends FormInstance<T>> extends AbstractContext {
    private final RemoteForm<T, F> form;

    public RemoteFormContext(RemoteForm<T, F> form) {
        this.form = form;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return form.form.logicsInstance;
    }

    public FormInstance getFormInstance() {
        return form.form;
    }

    public String getLogMessage() {
        return form.getLogMessage();
    }

    public void delayUserInteraction(ClientAction action) {
        form.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return form.requestUserInteraction(actions);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, form.getExportPort(), form.getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps) throws SQLException, SQLHandledException {
        return form.form.createForm(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive, contextFilters, initFilterProperty, pullProps);
    }
}
