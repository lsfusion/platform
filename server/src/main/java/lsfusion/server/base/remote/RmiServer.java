package lsfusion.server.base.remote;

import lsfusion.interop.server.RmiServerInterface;
import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.base.lifecycle.EventServer;
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
