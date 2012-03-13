package platform.server;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.base.BaseUtils;
import platform.interop.RemoteContextInterface;
import platform.interop.action.ClientAction;
import platform.interop.remote.RemoteObject;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

    @Override
    public void requestUserInteraction(ClientAction... actions) {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
    }

    public FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean newSession, boolean interactive) throws SQLException {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, boolean checkOnOk) {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
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
