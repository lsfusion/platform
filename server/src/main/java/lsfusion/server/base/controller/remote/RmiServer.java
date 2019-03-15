package lsfusion.server.base.controller.remote;

import lsfusion.interop.server.RmiServerInterface;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.controller.manager.EventServer;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;

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

    protected DataSession createSession() throws SQLException {
        ThreadLocalContext.assureRmi(this);
        return super.createSession();
    }
}
