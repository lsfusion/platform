package lsfusion.server;

import lsfusion.interop.ModalityType;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.context.Context;
import lsfusion.server.context.ExecutionStack;
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
import lsfusion.server.stack.ExecutionStackItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class WrapperContext extends AbstractContext implements Context {
    private Context wrappedContext;

    public void setContext(Context context) {
        wrappedContext = context;
    }

    public WrapperContext() {
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

    public PropertyObjectInterfaceInstance getComputer(ExecutionStack stack) {
        return wrappedContext.getComputer(stack);
    }

    public Integer getCurrentUser() {
        return wrappedContext.getCurrentUser();
    }

    public DataObject getConnection() {
        return wrappedContext.getConnection();
    }

    @Override
    public Locale getLocale() {
        return wrappedContext.getLocale();
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        return wrappedContext.createRemoteForm(formInstance, stack);
    }

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, ExecutionStack stack) throws SQLException, SQLHandledException {
        wrappedContext.requestFormUserInteraction(formInstance, modalityType, stack);
    }

    public ObjectValue requestUserObject(DialogRequest dialogRequest, ExecutionStack stack) throws SQLException, SQLHandledException {
        return wrappedContext.requestUserObject(dialogRequest, stack);
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

    public Thread getLastThread() {
        return wrappedContext.getLastThread();
    }

    public void pushActionMessage(ExecutionStackItem stackItem) {
        wrappedContext.pushActionMessage(stackItem);
    }

    public void popActionMessage(ExecutionStackItem stackItem) {
        wrappedContext.popActionMessage(stackItem);
    }
}
