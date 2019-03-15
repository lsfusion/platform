package lsfusion.server.logics.action.stack;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.Processor;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.language.linear.LP;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.classes.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.sets.ResolveClassSet;

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

    public void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        if(upStack != null && upStack.sameSession(session))
            upStack.updateCurrentClasses(session);
    }

    // nullable
    protected abstract DataSession getSession();

    public boolean sameSession(UpdateCurrentClassesSession session) {
        DataSession thisSession = getSession();
        return thisSession == null || session.sameSession(thisSession);
    }
}
