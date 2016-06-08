package lsfusion.server.context;

import lsfusion.base.Pair;
import lsfusion.base.Processor;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.List;

public interface ExecutionStack {

    ImMap<String, String> getAllParamsWithClassesInStack();

    ImMap<String, ObjectValue> getAllParamsWithValuesInStack();

    ImSet<Pair<LP, List<ResolveClassSet>>> getAllLocalsInStack();

    Processor<ImMap<String, ObjectValue>> getWatcher();

    boolean hasNewDebugStack();

    void updateOnApply(DataSession session) throws SQLException, SQLHandledException;

    void updateLastUserInput(DataSession session, final ObjectValue userInput);

    boolean sameSession(DataSession session);
}
