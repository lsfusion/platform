package platform.server;

import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.remote.RemoteDialog;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Map;

public class WrapperContext implements Context{
    Context wrappedContext;
    
    public BusinessLogics getBL() {
        return wrappedContext.getBL();
    }

    public FormInstance getFormInstance() {
        return wrappedContext.getFormInstance();
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

    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean interactive) throws SQLException {
        return wrappedContext.createFormInstance(formEntity, mapObjects, session, isModal, sessionScope, checkOnOk, interactive);
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

    public void delayRemoteChanges() {
        wrappedContext.delayRemoteChanges();
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
}
