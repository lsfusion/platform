package lsfusion.server.context;

import lsfusion.base.Pair;
import lsfusion.base.Processor;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.List;

public abstract class UpExecutionStack implements ExecutionStack {

    protected final ExecutionStack upStack;

    public UpExecutionStack(ExecutionStack upStack) {
        this.upStack = upStack;
        ServerLoggers.assertLog(upStack != null, "UPSTACK IS NULL");
    }

    public ImMap<String, String> getAllParamsWithClassesInStack() {
        if(upStack != null)
            return upStack.getAllParamsWithClassesInStack();
        return MapFact.EMPTY();
    }

    public ImMap<String, ObjectValue> getAllParamsWithValuesInStack() {
        if(upStack != null)
            return upStack.getAllParamsWithValuesInStack();
        return MapFact.EMPTY();
    }

    public ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack() {
        if(upStack != null)
            return upStack.getAllLocalsInStack();
        return SetFact.EMPTY();
    }

    public boolean hasNewDebugStack() {
        if(upStack != null)
            return upStack.hasNewDebugStack();
        return false;
    }

    public Processor<ImMap<String, ObjectValue>> getWatcher() {
        if(upStack != null)
            return upStack.getWatcher();
        return null;
    }

    public void updateOnApply(DataSession session) throws SQLException, SQLHandledException {
        if(upStack != null && upStack.sameSession(session))
            upStack.updateOnApply(session);
    }

    public void updateLastUserInput(DataSession session, final ObjectValue userInput) {
        if(upStack != null && upStack.sameSession(session))
            upStack.updateLastUserInput(session, userInput);
    }

    // nullable
    protected abstract DataSession getSession();

    public boolean sameSession(DataSession session) {
        DataSession thisSession = getSession();
        return thisSession == null || thisSession == session;
    }
}
