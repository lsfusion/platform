package lsfusion.server.remote;

import lsfusion.interop.remote.RmiServerInterface;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.lifecycle.EventServer;

public abstract class RmiServer extends EventServer implements RmiServerInterface {

    protected ExecutionStack getStack() { // вызов идет не из RemoteForm / RemoteNavigator / RemoteLogics
        ThreadLocalContext.assureRmi(this);
        return ThreadLocalContext.getStack();
    }

    public RmiServer() {
    }

    public RmiServer(int order) {
        super(order);
    }

}
