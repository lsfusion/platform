package platform.server;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import platform.base.BaseUtils;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.RemoteObject;

import java.rmi.RemoteException;
import java.util.Stack;

public abstract class RemoteContextObject extends RemoteObject implements Context, RemoteContextInterface {

    protected RemoteContextObject(int port) throws RemoteException {
        super(port);
    }

    public static String popCurrentActionMessage() {
        return Context.context.get().popActionMessage();
    }

    public static void pushCurrentActionMessage(String segment) {
        Context.context.get().pushActionMessage(segment);
    }

    public String getRemoteActionMessage() throws RemoteException {
        return Context.context.get().getActionMessage();
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

    public void initContext() {
        Context.context.set(this); // вообще должен быть null, но в силу thread pooling'а не всегда
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

//        @Before("execution(* (platform.interop.RemoteContextInterface || platform.interop.navigator.RemoteNavigatorInterface || platform.interop.form.RemoteFormInterface || platform.interop.RemoteLogicsInterface).*(..)) && target(remoteForm)")

    @Aspect
    public static class RemoteFormContextHoldingAspect {
        @Before("execution(* (platform.interop.RemoteContextInterface+ && platform.interop..*).*(..)) && " +
                "!cflowbelow(execution(* (platform.interop.RemoteContextInterface+ && platform.interop..*).*(..))) && target(remoteForm)")
        public void beforeCall(RemoteContextObject remoteForm) {
            remoteForm.initContext();
        }
    }

}
