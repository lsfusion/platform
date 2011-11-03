package platform.server;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.base.BaseUtils;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.RemoteObject;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
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
            return actionMessageStack.pop();
        }
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
