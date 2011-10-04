package platform.server;

import platform.base.BaseUtils;
import platform.interop.remote.RemoteObject;
import platform.server.logics.BusinessLogics;

import java.rmi.RemoteException;
import java.util.Stack;

public abstract class RemoteContextObject extends RemoteObject implements Context {

    protected RemoteContextObject(int port) throws RemoteException {
        super(port);
    }

    public static class MessageStack extends Stack<String> {
        public void set(String message) {
            clear();
            push(message);
        }

        public String pop() {
            return isEmpty() ? null : super.pop();
        }

        public String getMessage() {
            return  BaseUtils.toString(this, ". ");
        }
    }

    public MessageStack actionMessageStack = new MessageStack();

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
        return actionMessageStack.pop();
    }
}
