package platform.server.context;

import com.google.common.base.Throwables;
import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ChooseClassClientAction;
import platform.interop.action.ClientAction;
import platform.interop.action.DialogClientAction;
import platform.interop.action.RequestUserInputClientAction;
import platform.interop.form.UserInputResult;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormCloseType;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.remote.RemoteDialog;
import platform.server.remote.RemoteForm;
import platform.server.logics.*;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Stack;

import static platform.base.BaseUtils.serializeObject;
import static platform.server.data.type.TypeSerializer.serializeType;

public abstract class AbstractContext implements Context {
    public final MessageStack actionMessageStack = new MessageStack();

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormInstance getFormInstance() {
        return null;
    }

    public ObjectValue requestUserObject(ExecutionContext.RequestDialog dialog) throws SQLException { // null если canceled
        DialogInstance<?> dialogInstance = dialog.createDialog();
        if (dialogInstance == null) {
            return null;
        }

        RemoteDialog remoteDialog = createRemoteDialog(dialogInstance);
        requestUserInteraction(new DialogClientAction(remoteDialog));
        if (dialogInstance.getFormResult() == FormCloseType.CLOSE) {
            return null;
        }
        return dialogInstance.getFormResult() == FormCloseType.DROP ? NullValue.instance : dialogInstance.getDialogObjectValue();
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
    public void delayRemoteChanges() {
        throw new UnsupportedOperationException("delayRemoteChanges is not supported");
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

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive) throws SQLException {
        throw new UnsupportedOperationException("createFormInstance is not supported");
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }

    public RemoteDialog createRemoteDialog(DialogInstance dialogInstance) {
        throw new UnsupportedOperationException("createRemoteDialog is not supported");
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
}
