package lsfusion.server.logics.action.controller.stack;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.Processor;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.physics.admin.log.ServerLoggers;

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

    @Override
    public void dropPushAsyncResult() {
        if(upStack != null)
            upStack.dropPushAsyncResult();
    }

    // nullable
    protected abstract DataSession getSession();

    public boolean sameSession(UpdateCurrentClassesSession session) {
        DataSession thisSession = getSession();
        return thisSession == null || session.sameSession(thisSession);
    }

    @Override
    public EvalScriptingLogicsModule getEvalLM() {
        if(upStack != null)
            return upStack.getEvalLM();
        return null;
    }
}
