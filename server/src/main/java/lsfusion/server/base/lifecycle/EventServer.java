package lsfusion.server.base.lifecycle;

import lsfusion.server.base.context.NewThreadExecutionStack;
import lsfusion.server.base.context.TopExecutionStack;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.session.DataSession;

import java.sql.SQLException;

public abstract class EventServer extends LifecycleAdapter {

    public EventServer() {
    }

    public EventServer(int order) {
        super(order);
    }

    public abstract String getEventName();

    private final NewThreadExecutionStack stack = new TopExecutionStack(getEventName());
    public NewThreadExecutionStack getTopStack() {
        return stack;
    }

    public abstract LogicsInstance getLogicsInstance();

    protected DataSession createSession() throws SQLException {
        return getLogicsInstance().getDbManager().createSession();
    }
}
