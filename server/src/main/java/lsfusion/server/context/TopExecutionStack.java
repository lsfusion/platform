package lsfusion.server.context;

import lsfusion.base.Pair;
import lsfusion.base.Processor;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.List;

public class TopExecutionStack implements NewThreadExecutionStack {

    private final String threadId;

    public TopExecutionStack(String threadId) {
        this.threadId = threadId;
    }

    @Override
    public ImMap<String, String> getAllParamsWithClassesInStack() {
        return MapFact.EMPTY();
    }

    @Override
    public ImMap<String, ObjectValue> getAllParamsWithValuesInStack() {
        return MapFact.EMPTY();
    }

    @Override
    public ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack() {
        return SetFact.EMPTY();
    }

    @Override
    public Processor<ImMap<String, ObjectValue>> getWatcher() {
        return null;
    }

    @Override
    public boolean hasNewDebugStack() {
        return false;
    }

    @Override
    public void updateOnApply(DataSession session) throws SQLException, SQLHandledException {
    }

    @Override
    public void updateLastUserInput(DataSession session, ObjectValue userInput) {
    }

    @Override
    public boolean sameSession(DataSession session) {
        return true; // особо не принципиально можно и false
    }

    @Override
    public boolean checkStack(NewThreadExecutionStack stack) {
        if(!(stack instanceof TopExecutionStack))
            return false;
        return threadId.equals(((TopExecutionStack)stack).threadId);
    }

    @Override
    public String toString() {
        return threadId + "@" + System.identityHashCode(this);
    }
}
