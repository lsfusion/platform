package lsfusion.server.context;

import lsfusion.base.Pair;
import lsfusion.base.lambda.Processor;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.action.session.UpdateCurrentClassesSession;

import java.sql.SQLException;
import java.util.List;

public interface ExecutionStack {

    ImMap<String, String> getAllParamsWithClassesInStack();

    ImMap<String, ObjectValue> getAllParamsWithValuesInStack();

    ImSet<Pair<LCP, List<ResolveClassSet>>> getAllLocalsInStack();

    Processor<ImMap<String, ObjectValue>> getWatcher();

    boolean hasNewDebugStack();

    void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException;

    boolean sameSession(UpdateCurrentClassesSession session);
}
