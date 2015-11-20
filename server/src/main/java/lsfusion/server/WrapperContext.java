package lsfusion.server;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.context.Context;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.remote.RemoteForm;

import java.sql.SQLException;
import java.util.List;

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

    public SecurityPolicy getSecurityPolicy() {
        return wrappedContext.getSecurityPolicy();
    }

    public FocusListener getFocusListener() {
        return wrappedContext.getFocusListener();
    }

    public CustomClassListener getClassListener() {
        return wrappedContext.getClassListener();
    }

    public PropertyObjectInterfaceInstance getComputer() {
        return wrappedContext.getComputer();
    }

    public Integer getCurrentUser() {
        return wrappedContext.getCurrentUser();
    }

    public DataObject getConnection() {
        return wrappedContext.getConnection();
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        return wrappedContext.createRemoteForm(formInstance);
    }

    public ObjectValue requestUserObject(DialogRequest dialogRequest) throws SQLException, SQLHandledException {
        return wrappedContext.requestUserObject(dialogRequest);
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

    public LogInfo getLogInfo() {
        return wrappedContext.getLogInfo();
    }

    public void delayUserInteraction(ClientAction action) {
        wrappedContext.delayUserInteraction(action);
    }

    public Object requestUserInteraction(ClientAction action) {
        return wrappedContext.requestUserInteraction(action);
    }

    @Override
    public boolean canBeProcessed() {
        return false;
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return wrappedContext.requestUserInteraction(actions);
    }

    public String getActionMessage() {
        return wrappedContext.getActionMessage();
    }

    public List<Object> getActionMessageList() {
        return wrappedContext.getActionMessageList();
    }

    public void pushActionMessage(Object segment) {
        wrappedContext.pushActionMessage(segment);
    }

    public Object popActionMessage() {
        return wrappedContext.popActionMessage();
    }
}
