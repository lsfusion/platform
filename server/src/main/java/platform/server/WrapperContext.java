package platform.server;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.context.AbstractContext;
import platform.server.context.Context;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.logics.LogicsInstance;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ExecutionContext;
import platform.server.remote.RemoteDialog;
import platform.server.remote.RemoteForm;
import platform.server.session.DataSession;

import java.sql.SQLException;

public class WrapperContext extends AbstractContext implements Context {
    private final Context wrappedContext;

    public WrapperContext(Context wrappedContext) {
        this.wrappedContext = wrappedContext;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return wrappedContext.getLogicsInstance();
    }

    public FormInstance getFormInstance() {
        return wrappedContext.getFormInstance();
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        return wrappedContext.createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, showDrop, interactive);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        return wrappedContext.createRemoteForm(formInstance);
    }

    public RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        return wrappedContext.createRemoteDialog(dialogInstance);
    }

    public ObjectValue requestUserObject(ExecutionContext.RequestDialog requestDialog) throws SQLException {
        return wrappedContext.requestUserObject(requestDialog);
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        return wrappedContext.requestUserData(dataClass, oldValue);
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        return wrappedContext.requestUserClass(baseClass, defaultValue, concrete);
    }

    public String getLogMessage() {
        return wrappedContext.getLogMessage();
    }

    public void delayUserInteraction(ClientAction action) {
        wrappedContext.delayUserInteraction(action);
    }

    public Object requestUserInteraction(ClientAction action) {
        return wrappedContext.requestUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return wrappedContext.requestUserInteraction(actions);
    }

    public void setActionMessage(String message) {
        wrappedContext.setActionMessage(message);
    }

    public String getActionMessage() {
        return wrappedContext.getActionMessage();
    }

    public void pushActionMessage(String segment) {
        wrappedContext.pushActionMessage(segment);
    }

    public String popActionMessage() {
        return wrappedContext.popActionMessage();
    }
}
