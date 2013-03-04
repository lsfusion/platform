package platform.server;

import com.google.common.base.Throwables;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.RemoteContextInterface;
import platform.interop.action.ChooseClassClientAction;
import platform.interop.action.ClientAction;
import platform.interop.action.DialogClientAction;
import platform.interop.action.RequestUserInputClientAction;
import platform.interop.form.UserInputResult;
import platform.interop.remote.RemoteObject;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormCloseType;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.form.instance.remote.RemoteDialog;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static platform.base.BaseUtils.serializeObject;
import static platform.server.data.type.TypeSerializer.serializeType;

public abstract class RemoteContextObject extends RemoteObject implements Context, RemoteContextInterface {

    public List<Thread> threads = new ArrayList<Thread>();

    protected RemoteContextObject(int port) throws RemoteException {
        super(port);
    }

    public static String popCurrentActionMessage() {
        if (Context.context.get() != null)
            return Context.context.get().popActionMessage();
        else
            return "";
    }

    public static void pushCurrentActionMessage(String segment) {
        if (Context.context.get() != null)
            Context.context.get().pushActionMessage(segment);
    }

    public String getRemoteActionMessage() throws RemoteException {
        if (Context.context.get() != null)
            return Context.context.get().getActionMessage();
        else
            return "";
    }

    public class MessageStack extends Stack<String> {
        public void set(String message) {
            clear();
            push(message);
        }

        public String getMessage() {
            return  BaseUtils.toString(this, ". ");
        }
    }

    public final MessageStack actionMessageStack = new MessageStack();

    public void setActionMessage(String message) {
        synchronized (actionMessageStack) {
            actionMessageStack.set(message);
        }
    }

    public String getActionMessage() {
        synchronized (actionMessageStack) {
            return actionMessageStack.getMessage();
        }
    }

    public void pushActionMessage(String segment) {
        synchronized (actionMessageStack) {
            actionMessageStack.push(segment);
        }
    }

    public String popActionMessage() {
        synchronized (actionMessageStack) {
            if (!actionMessageStack.isEmpty())
                return actionMessageStack.pop();
            else
                return "";
        }
    }

    public ObjectValue requestUserObject(ExecutionContext.RequestDialog dialog) throws SQLException { // null если canceled
        DialogInstance<?> dialogInstance = dialog.createDialog();
        if(dialogInstance==null)
            return null;

        RemoteDialog remoteDialog = createRemoteDialog(dialogInstance);
        requestUserInteraction(new DialogClientAction(remoteDialog));
        if(dialogInstance.getFormResult() == FormCloseType.CLOSE)
            return null;
        return dialogInstance.getFormResult() == FormCloseType.DROP ? NullValue.instance : dialogInstance.getDialogObjectValue();
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue)));
            if(result.isCanceled())
                return null;
            return result.getValue()==null?NullValue.instance:new DataObject(result.getValue(), dataClass);
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
            if(result==null)
                return null;
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

    public void killThreads() {
        if (Settings.instance.getKillThread())
            for (Thread thread : threads) {
                thread.stop();
            }
    }

//        @Before("execution(* (platform.interop.RemoteContextInterface || platform.interop.navigator.RemoteNavigatorInterface || platform.interop.form.RemoteFormInterface || platform.interop.RemoteLogicsInterface).*(..)) && target(remoteForm)")

    @Aspect
    public static class RemoteFormContextHoldingAspect {
        final String aspectArgs = "execution(* (platform.interop.RemoteContextInterface+ && platform.interop..*).*(..)) &&" +
                "!cflowbelow(execution(* (platform.interop.RemoteContextInterface+ && platform.interop..*).*(..))) && " +
                "!cflowbelow(initialization(platform.server.logics.BusinessLogics.new(..))) && target(remoteObject)";

        @Before(aspectArgs)
        public void beforeCall(RemoteContextObject remoteObject) {
            if(!(Thread.currentThread() instanceof ContextAwareThread)) {
                Context.context.set(remoteObject); // вообще должен быть null, но в силу thread pooling'а не всегда
                remoteObject.threads.add(Thread.currentThread());
            }
        }

        @AfterReturning(aspectArgs)
        public void afterReturn(RemoteContextObject remoteObject) {
            remoteObject.threads.remove(Thread.currentThread());
        }
    }
}
