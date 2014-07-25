package lsfusion.server.context;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.UserInputResult;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.UpdateCurrentClasses;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class AbstractContext implements Context {
    public final MessageStack actionMessageStack = new MessageStack();

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormInstance getFormInstance() {
        return null;
    }

    public ObjectValue requestUserObject(DialogRequest dialog) throws SQLException, SQLHandledException { // null если canceled
        FormInstance dialogInstance = dialog.createDialog();
        if (dialogInstance == null) {
            return null;
        }

        requestUserInteraction(
                new FormClientAction(
                        dialogInstance.entity.getCanonicalName(),
                        dialogInstance.entity.getSID(),
                        createRemoteForm(dialogInstance),
                        ModalityType.DIALOG_MODAL));
        
        if (dialogInstance.getFormResult() == FormCloseType.CLOSE) {
            return null;
        }
        return dialogInstance.getFormResult() == FormCloseType.DROP ? NullValue.instance : dialog.getValue();
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue)));
            if (result.isCanceled()) {
                return null;
            }
            return result.getValue() == null ? NullValue.instance : new DataObject(result.getValue(), dataClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            baseClass.serialize(dataStream);
            defaultValue.serialize(dataStream);
            Integer result = (Integer) requestUserInteraction(new ChooseClassClientAction(outStream.toByteArray(), concrete));
            if (result == null) {
                return null;
            }
            return new DataObject(result, baseClass.getBaseClass().objectClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String getLogMessage() {
        throw new UnsupportedOperationException("getLogMessage is not supported");
    }
    
    @Override
    public void delayUserInteraction(ClientAction action) {
        throw new UnsupportedOperationException("delayUserInteraction is not supported");
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        return requestUserInteraction(new ClientAction[]{action})[0];
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, UpdateCurrentClasses outerUpdateCurrentClasses, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException("createFormInstance is not supported");
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }

    public void setActionMessage(String message) {
        actionMessageStack.set(message);
    }

    public String getActionMessage() {
        return actionMessageStack.getMessage();
    }

    public void pushActionMessage(String segment) {
        actionMessageStack.push(segment);
    }

    public String popActionMessage() {
        return actionMessageStack.popOrEmpty();
    }

    private static class MessageStack extends Stack<String> {
        public synchronized void set(String message) {
            clear();
            push(message);
        }

        public synchronized String getMessage() {
            return BaseUtils.toString(this, ". ");
        }

        public synchronized String popOrEmpty() {
            return isEmpty() ? "" : pop();
        }
    }

    private ScheduledExecutorService executor;
    @Override
    public ScheduledExecutorService getExecutorService() {
        if(executor==null)
            synchronized (this) { 
                if(executor==null)
                    executor = Executors.newScheduledThreadPool(50, new ContextAwareDaemonThreadFactory(this, "newthread-pool"));
            }
        return executor;
    }
}
