package platform.server.remote;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.server.context.AbstractContext;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsInstance;
import platform.server.session.DataSession;

import java.rmi.RemoteException;
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

    public void delayRemoteChanges() {
        form.delayRemoteChanges();
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
    public RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        try {
            return new RemoteDialog(dialogInstance, form.getExportPort(), form.getRemoteFormListener());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        return form.form.createForm(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive);
    }
}
